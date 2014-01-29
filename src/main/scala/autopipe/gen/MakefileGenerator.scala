package autopipe.gen

import autopipe._
import scala.collection.mutable.ListBuffer
import java.io.{File, FileOutputStream, PrintStream}

private[autopipe] class MakefileGenerator(
        val ap: AutoPipe
    ) extends Generator {

    private def emitKernelMakefile(dir: File,
                                   name: String,
                                   platforms: Set[Platforms.Value]) {

        // Make the directory.
        val subdir = new File(dir, name)
        subdir.mkdir

        // Generate the Makefile.
        val makeFile = new File(subdir, "Makefile")
        val makePS = new PrintStream(new FileOutputStream(makeFile))
        if (platforms.contains(Platforms.HDL)) {
            makePS.println("V_FILES=" + name + ".v")
        }
        if (platforms.contains(Platforms.C)) {
            makePS.println("C_FILES=" + name + ".c")
        }
        makePS.println("""
# Get the source files.
get_files:
	echo $(addprefix $(BLKDIR)/,$(C_FILES)) >> $(C_FILE_LIST)
	echo $(addprefix $(BLKDIR)/,$(CXX_FILES)) >> $(CXX_FILE_LIST)
	echo $(addprefix $(BLKDIR)/,$(VHDL_FILES)) >> $(VHDL_FILE_LIST)
	echo $(addprefix $(BLKDIR)/,$(V_FILES)) >> $(V_FILE_LIST)

# Generate the synthesis file.
synfile:
	$(foreach b,$(VHDL_FILES), \
		/bin/echo "add_file -vhdl \"$(BLKPATH)/$b\"" >> $(PRJFILE);)
	$(foreach b,$(V_FILES), \
		/bin/echo "add_file -verilog \"$(BLKPATH)/$b\"" >> $(PRJFILE);)

clean:
	rm -f *.o""")
        makePS.close


    }

    def emit(dir: File) {

        // Get a list of kernel types.
        val kernelTypes = ap.getKernelTypes()

        // Get the set of devices.
        var devices = ap.instances.map(_.device).toSet

        // Get the set of platforms for each kernel type.
        var platformMap = Map[String, Set[Platforms.Value]]()
        devices.foreach { d =>
            val platform = d.platform
            ap.getKernelTypes(d).filter(_.internal).foreach { kt =>
                val name = kt.name
                val ps = platformMap.getOrElse(name, Set[Platforms.Value]())
                val ns = ps + platform
                platformMap = platformMap + (name -> ns)
            }
        }

        // Emit Makefiles for the kernels.
        kernelTypes.filter(_.internal).map(_.name).foreach { kt =>
            emitKernelMakefile(dir, kt, platformMap(kt))
        }

        // Get a list of libraries.
        val libraries = kernelTypes.flatMap { kt =>
            kt.dependencies.get(DependencySet.Library)
        }

        // Get a list of library paths.
        val lpaths = kernelTypes.flatMap { kt =>
            kt.dependencies.get(DependencySet.LPath)
        }

        // Get a list of include paths.
        val ipaths = kernelTypes.flatMap { kt =>
            kt.dependencies.get(DependencySet.IPath)
        }

        // Get a list of local C kernels.
        val localC = kernelTypes.filter { kt =>
            kt.platform == Platforms.C && kt.internal
        }

        // Get a list of local HDL kernels.
        val localHDL = kernelTypes.filter { kt =>
            kt.platform == Platforms.HDL && kt.internal
        }

        // Determine if we need to link in TimeTrial.
        val needTimeTrial = !ap.streams.filter(!_.measures.isEmpty).isEmpty

        // Get a list of processes to create (one for each host).
        val targets = ap.devices.map { d => "proc_" + d.host }

        write("TARGETS=" + targets.mkString(" "))
        write("C_BLOCKS=" + localC.mkString(" "))
        write("FPGA_BLOCKS=" + localHDL.mkString(" "))
        write("TTOBJ=" + (if (needTimeTrial) "tta.o" else ""))

        val ipaths_str = ipaths.foldLeft("") { (a, p) => a + " -I" + p }
        write("EXTRA_CFLAGS=" + ipaths_str)
        write("EXTRA_CXXFLAGS=" + ipaths_str)

        val lpaths_str = lpaths.foldLeft("") { (a, p) => a + " -L" + p }
        val libs_str = libraries.foldLeft("") { (a, l) => a + " -l" + l }
        write("EXTRA_LDFLAGS=" + lpaths_str + libs_str)

        write("""

# Determine the full path to the project.
ifndef THIS
    export THIS:=$(shell pwd)
endif

BLOCKS := $(sort $(FPGA_BLOCKS) $(C_BLOCKS))
BLOCK_CINC := $(foreach x,$(C_BLOCKS),-I$(THIS)/$x)
BLOCK_XINC := $(foreach x,$(BLOCKS),-I$(THIS)/$x)

# Determine our architecture.
ARCH := $(shell uname -s)-$(shell uname -m)

INCS = -I$(THIS)
export CFLAGS = -Wall -O2 $(INCS) $(EXTRA_CFLAGS) $(USER_CFLAGS)
export CXXFLAGS = $(CFLAGS) $(EXTRA_CFLAGS) $(USER_CXXFLAGS) 
export CC=gcc
export CXX=g++
export LDFLAGS=-lpthread $(EXTRA_LDFLAGS) $(USER_LDFLAGS)
export C_FILE_LIST=$(THIS)/.c_file_list
export CXX_FILE_LIST=$(THIS)/.cxx_file_list
export VHDL_FILE_LIST=$(THIS)/.vhdl_file_list
export V_FILE_LIST=$(THIS)/.v_file_list

CXXFILES=$(strip $(foreach f,$(shell cat $(CXX_FILE_LIST) 2>/dev/null),$f))
CFILES=$(strip $(foreach f,$(shell cat $(C_FILE_LIST) 2>/dev/null),$f))
ALL_C_FILES=$(CXXFILES) $(CFILES)
OBJECTS = $(foreach f,$(ALL_C_FILES),$(addsuffix .o,$(basename $f))) $(TTOBJ)

# Compile by default
all: compile

flow: compile
	$(MAKE) syn
	$(MAKE) build

help:
	@echo
	@echo "Makefile targets:"
	@echo "    all              Same as compile (default target)"
	@echo "    compile          Check out blocks and compile"
	@echo "    update           Update blocks to the latest revision"
	@echo "    syn              Synthesize HDL"
	@echo "    build            Generate a bitfile"
	@echo "    flow             Same as \"make syn build\""
	@echo "    sim              Simluate HDL"
	@echo "    install          Install the bitfile"
	@echo "    clean            Remove generated files"
	@echo "    distclean        Remove generated files and block checkouts"
	@echo "    help             Display this message"
	@echo

# Check out and build the blocks.
.PHONY: blocks
blocks:
	$(foreach b,$(BLOCKS), $(MAKE) $(b);)
	rm -f $(V_FILE_LIST) $(VHDL_FILE_LIST) $(C_FILE_LIST) $(CXX_FILE_LIST)
	$(foreach b,$(BLOCKS),\
	    (cd $b ; $(MAKE) get_files "BLKDIR=$(THIS)/$b");)

.PHONY: clean_blocks
clean_blocks:
	$(foreach b,$(C_BLOCKS), \
		(if [ -e $(b) ] ; then \
			cd $(b) && $(MAKE) -i clean ; \
		fi);)

# Rule for compiling everything.
compile: blocks
	$(MAKE) $(TARGETS)

# Rule for compiling C++ code.
%.o: %.cpp
	$(CXX) -c $(CXXFLAGS) $(BLOCK_CINC) -o $@ $<

# Rule for compiling C code.
%.o: %.c
	$(CC) -c $(CFLAGS) $(BLOCK_CINC) -o $@ $<

# Rule for creating the proc_* executables.
proc_%: proc_%.o $(OBJECTS)
	$(CXX) $(CXXFLAGS) -o $@ $@.o $(OBJECTS) $(LDFLAGS)

""")

        writeLeft(ap.getRules)

        write("""
# Rule for cleaning up.
clean: clean_blocks
	rm -f $(TARGETS) proc_*.o $(VHDL_FILE_LIST) $(V_FILE_LIST) $(C_FILE_LIST) $(CXX_FILE_LIST) dump.vcd

# Rule for cleaning up everything.
distclean: clean
	$(foreach b,$(C_BLOCKS), rm -rf $b;)
	$(foreach b,$(FPGA_BLOCKS), rm -rf $b;)
""")

        writeFile(dir, "Makefile")

    }

}
