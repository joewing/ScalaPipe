

package autopipe.gen

import autopipe._

private[autopipe] class MakefileGenerator extends Generator {

   def emit(ap: AutoPipe) {

      // Get a list of block types.
      val blockTypes = ap.blocks.map(_.blockType).distinct

      // Get a list of functions.
      val functions = ap.functions

      // Get a list of libraries.
      val libraries = blockTypes.flatMap { bt =>
         bt.dependencies.get(DependencySet.Library)
      }

      // Get a list of library paths.
      val lpaths = blockTypes.flatMap { bt =>
         bt.dependencies.get(DependencySet.LPath)
      }

      // Get a list of include paths.
      val ipaths = blockTypes.flatMap { bt =>
         bt.dependencies.get(DependencySet.IPath)
      }

      // Get a list of local C blocks.
      val localC = blockTypes.filter { bt =>
         bt.platform == Platforms.C && bt.internal
      }

      // Get a list of external C blocks.
      val externalC = blockTypes.filter { bt =>
         bt.platform == Platforms.C && !bt.internal
      }

      // Get a list of local HDL blocks.
      val localHDL = blockTypes.filter { bt =>
         bt.platform == Platforms.HDL && bt.internal
      }

      // Get a list of external HDL blocks.
      val externalHDL = blockTypes.filter { bt =>
         bt.platform == Platforms.HDL && !bt.internal
      }

      // Get a list of internal C and HDL functions.
      val cFunctions = functions.filter { ft =>
         ft.platform == Platforms.C && ft.internal
      }
      val hdlFunctions = functions.filter { ft =>
         ft.platform == Platforms.HDL && ft.internal
      }

      // Determine if we need to link in TimeTrial.
      val needTimeTrial = !ap.streams.filter(!_.measures.isEmpty).isEmpty

      // Get a list of processes to create (one for each host).
      val targets = ap.devices.map { d => "proc_" + d.host }

      write("TARGETS=" + targets.mkString(" "))
      write("LOCAL_C_BLOCKS=" + localC.mkString(" ") + " " +
                                cFunctions.mkString(" "))
      write("C_BLOCKS=" + externalC.mkString(" "))
      write("LOCAL_FPGA_BLOCKS=" + localHDL.mkString(" ") + " " +
                                   hdlFunctions.mkString(" "))
      write("FPGA_BLOCKS=" + externalHDL.mkString(" "))
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

ALL_C_BLOCKS := $(C_BLOCKS) $(LOCAL_C_BLOCKS)
ALL_FPGA_BLOCKS := $(FPGA_BLOCKS) $(LOCAL_FPGA_BLOCKS)
BLOCKS := $(ALL_FPGA_BLOCKS) $(ALL_C_BLOCKS)
BLOCK_CINC := $(foreach x,$(ALL_C_BLOCKS),-I$(THIS)/$x-dir)
BLOCK_XINC := $(foreach x,$(BLOCKS),-I$(THIS)/$x-dir)

# Determine our architecture.
ARCH := $(shell uname -s)-$(shell uname -m)

# Get site-specific settings.
# We pull in any user-specified settings and then read the
# default file to pull in any settings that haven't been specified.
ifdef X_SITE_CONFIG
   include $(X_SITE_CONFIG)
endif

ifdef $(SSH)
   BLOCK_REPO ?= svn+ssh://$(SSH)@cinnabox.int.seas.wustl.edu/project/mercury/auto-pipe/xblocks/trunk
else
   BLOCK_REPO ?= file:///project/mercury/auto-pipe/xblocks/trunk
endif

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
	@echo "   all               Same as compile (default target)"
	@echo "   compile           Check out blocks and compile"
	@echo "   update            Update blocks to the latest revision"
	@echo "   syn               Synthesize HDL"
	@echo "   build             Generate a bitfile"
	@echo "   flow              Same as \"make syn build\""
	@echo "   sim               Simluate HDL"
	@echo "   install           Install the bitfile"
	@echo "   clean             Remove generated files"
	@echo "   distclean         Remove generated files and block checkouts"
	@echo "   help              Display this message"
	@echo
	@echo "Note: Set the X_SITE_CONFIG enviroment variable to the"
	@echo "full-path of site-specific settings.  See site.makefile in the"
	@echo "makefiles directory of the xapps repository for an example."
	@echo

# Check out and build the blocks.
.PHONY: blocks
blocks:
	$(foreach b,$(BLOCKS), $(MAKE) $(b)-dir;)
	rm -f $(V_FILE_LIST) $(VHDL_FILE_LIST) $(C_FILE_LIST) $(CXX_FILE_LIST)
	$(foreach b,$(BLOCKS),\
	   (cd $b-dir ; $(MAKE) get_files "BLKDIR=$(THIS)/$b-dir");)

.PHONY: clean_blocks
clean_blocks:
	$(foreach b,$(ALL_C_BLOCKS), \
		(if [ -e $(b)-dir ] ; then \
			cd $(b)-dir && $(MAKE) -i clean ; \
		fi);)

# Rule to check out a block.
BVERSION = $(if $($*_VERSION),$($*_VERSION),HEAD)
%-dir:
	svn co -r $(BVERSION) $(BLOCK_REPO)/$* $@

# Rule to update to the requested revision of a block.
%-update: %-dir
	(cd $*-dir && svn up -r $(BVERSION))

# Rule to update all blocks.
update:
	$(foreach b,$(C_BLOCKS), $(MAKE) $(b)-update;)
	$(foreach b,$(FPGA_BLOCKS), $(MAKE) $(b)-update;)

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
	$(foreach b,$(C_BLOCKS), rm -rf $b-dir;)
	$(foreach b,$(FPGA_BLOCKS), rm -rf $b-dir;)
""")

   }

}

