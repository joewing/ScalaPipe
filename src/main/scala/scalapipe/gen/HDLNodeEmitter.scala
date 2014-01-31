package scalapipe.gen

import scalapipe._

private[gen] abstract class HDLNodeEmitter(
        _kt: KernelType,
        val graph: IRGraph,
        val moduleEmitter: HDLModuleEmitter)
    extends NodeEmitter(_kt) with HDLGenerator {

    protected var initializers = Set[Generator]()
    protected var updaters = Set[Generator]()

    def checkPorts(block: StateBlock): Unit
    def releasePorts(block: StateBlock): Unit
    def emitAvailable(block: StateBlock, node: IRInstruction): Unit
    def emitAssign(block: StateBlock, node: IRInstruction): Unit
    def emitStop(block: StateBlock, node: IRStop): Unit
    def emitReturn(block: StateBlock, node: IRReturn): Unit
    def start: Unit
    def stop: Unit

    private def addGuard(block: StateBlock, guard: String) {
        moduleEmitter.addGuard(block.label, guard)
    }

    private def emitBegin(block: StateBlock) {
        write(s"if (state == ${block.label}) begin")
        enter
        beginScope
        checkPorts(block)
        write(s"if (guard_${block.label}) begin")
        enter
    }

    private def emitEnd(block: StateBlock) {
        releasePorts(block)
        leave
        write("end")
        endScope
        initializers.foreach { i =>
            write("if (state != last_state) begin")
            enter
            write(i)
            leave
            write("end")
        }
        initializers = Set()
        updaters.foreach { u =>
            write(u)
        }
        updaters = Set()
        leave
        write("end")
    }

    private def shiftRight(sym: BaseSymbol, shift: Int): String = {
        sym match {
            case im: ImmediateSymbol => (im.value.long >> shift).toString
            case _ => "(" + emitSymbol(sym) + " >>> " + shift + ")"
        }
    }

    private def emitIntConvert(block: StateBlock,
                               vt: IntegerValueType,
                               node: IRInstruction): String = {
        val srcWidth = node.srca.valueType.bits
        val destWidth = node.dest.valueType.bits
        val srca = emitSymbol(node.srca)
        node.srca.valueType match {
            case ivt: IntegerValueType if srcWidth >= destWidth =>
                val top = destWidth - 1
                s"$srca[$top:0]"
            case ivt: IntegerValueType if srcWidth < destWidth =>
                val repeat = destWidth - srcWidth
                if (ivt.signed) {
                    s"{{$repeat{$srca[${srcWidth - 1}]}},$srca}"
                } else {
                    s"{{$repeat{1'b0}},$srca}"
                }
            case fvt: FloatValueType =>
                moduleEmitter.create("ap_ftoi" + srcWidth, srcWidth,
                                     block.label, Seq(srca))
            case _ => sys.error("internal: " + node.srca.valueType)
        }
    }

    private def emitIntOp(block: StateBlock,
                          vt: IntegerValueType,
                          node: IRInstruction) {
        val dest = emitSymbol(node.dest)
        val srca = emitSymbol(node.srca)
        val srcb = emitSymbol(node.srcb)
        val width = vt.bits
        val state = block.label
        val expression = node.op match {
            case NodeType.neg               => s"-$srca"
            case NodeType.not               => s"!$srca"
            case NodeType.compl             => s"~$srca"
            case NodeType.land              => s"$srca && $srcb"
            case NodeType.lor               => s"$srca || $srcb"
            case NodeType.and               => s"$srca & $srcb"
            case NodeType.or                => s"$srca | $srcb"
            case NodeType.xor               => s"$srca ^ $srcb"
            case NodeType.shr if vt.signed  => s"$srca >>> $srcb"
            case NodeType.shr if !vt.signed => s"$srca >> $srcb"
            case NodeType.shl if vt.signed  => s"$srca <<< $srcb"
            case NodeType.shl if !vt.signed => s"$srca << $srcb"
            case NodeType.add   =>
                moduleEmitter.createSimple("ap_addI", width, Seq(srca, srcb))
            case NodeType.sub   =>
                moduleEmitter.createSimple("ap_subI", width, Seq(srca, srcb))
            case NodeType.mul if node.srca.isInstanceOf[ImmediateSymbol] =>
                s"$srca * $srcb"
            case NodeType.mul if !node.srca.isInstanceOf[ImmediateSymbol] =>
                moduleEmitter.create("ap_mulI", width, state, Seq(srca, srcb))
            case NodeType.div if vt.signed =>
                moduleEmitter.create("ap_divS", width, state, Seq(srca, srcb))
            case NodeType.div if !vt.signed =>
                moduleEmitter.create("ap_divU", width, state, Seq(srca, srcb))
            case NodeType.mod if vt.signed =>
                moduleEmitter.create("ap_modS", width, state, Seq(srca, srcb))
            case NodeType.mod if !vt.signed =>
                moduleEmitter.create("ap_modU", width, state, Seq(srca, srcb))
            case NodeType.eq        => s"$srca == $srcb"
            case NodeType.ne        => s"$srca != $srcb"
            case NodeType.gt        => s"$srca > $srcb"
            case NodeType.lt        => s"$srca < $srcb"
            case NodeType.ge        => s"$srca >= $srcb"
            case NodeType.le        => s"$srca <= $srcb"
            case NodeType.convert   => emitIntConvert(block, vt, node)
            case NodeType.abs       => s"$srca < 0 ? -$srca : $srca"
            case NodeType.exp       =>
                moduleEmitter.create("ap_expI", width, state, List(srca))
            case NodeType.log       =>
                moduleEmitter.create("ap_logI", width, state, List(srca))
            case NodeType.sqrt      =>
                moduleEmitter.create("ap_sqrtI", width, state, List(srca))
            case NodeType.sin       =>
                moduleEmitter.create("ap_sinI", width, state, List(srca))
            case NodeType.cos       =>
                moduleEmitter.create("ap_cosI", width, state, List(srca))
            case NodeType.tan       =>
                moduleEmitter.create("ap_tanI", width, state, List(srca))
            case _ =>
                Error.raise("unsupported integer operation: " + node.op)
        }
        if (block.continuous) {
            moduleEmitter.addAssignment(s"$dest <= $expression;")
        } else {
            write(s"$dest <= $expression;")
        }
    }

    private def emitFixedOp(block: StateBlock, vt: FixedValueType,
                                    node: IRInstruction) {
        val dest = emitSymbol(node.dest)
        val srca = emitSymbol(node.srca)
        val srcb = emitSymbol(node.srcb)
        val width = vt.bits
        val state = block.label
        val frac = vt.fraction
        val f1 = frac / 2
        val f2 = (frac + 1) / 2
        val expression = node.op match {
            case NodeType.neg   => "-" + srca
            case NodeType.not   => "!" + srca
            case NodeType.compl => "~" + srca
            case NodeType.land  => srca + " && " + srcb
            case NodeType.lor   => srca + " || " + srcb
            case NodeType.and   => srca + " & " + srcb
            case NodeType.or    => srca + " | " + srcb
            case NodeType.xor   => srca + " ^ " + srcb
            case NodeType.shr   => srca + " >>> " + srcb
            case NodeType.shl   => srca + " <<< " + srcb
            case NodeType.add   =>
                moduleEmitter.createSimple("ap_addI", width, List(srca, srcb))
            case NodeType.sub   =>
                moduleEmitter.createSimple("ap_subI", width, List(srca, srcb))
            case NodeType.mul if node.srca.isInstanceOf[ImmediateSymbol] => 
                val ina = shiftRight(node.srca, f1)
                val inb = shiftRight(node.srcb, f2)
                s"$ina * $inb"
            case NodeType.mul if !node.srca.isInstanceOf[ImmediateSymbol] =>
                val ina = shiftRight(node.srca, f1)
                val inb = shiftRight(node.srcb, f2)
                moduleEmitter.create("ap_mulI", width, state, List(ina, inb))
            case NodeType.div =>
                val ina = srca
                val inb = shiftRight(node.srcb, f1)
                val out = moduleEmitter.create("ap_divS", width, state,
                                                         List(ina, inb))
                out + " <<< " + f2
            case NodeType.eq      => srca + " == " + srcb
            case NodeType.ne      => srca + " != " + srcb
            case NodeType.gt      => srca + " > " + srcb
            case NodeType.lt      => srca + " < " + srcb
            case NodeType.ge      => srca + " >= " + srcb
            case NodeType.le      => srca + " <= " + srcb
            case NodeType.abs     => srca + " < 0 ? -" + srca + " : " + srca
            case NodeType.exp     =>
                moduleEmitter.create("ap_expI", width, state, List(srca))
            case NodeType.log     =>
                moduleEmitter.create("ap_logI", width, state, List(srca))
            case NodeType.sqrt    =>
                moduleEmitter.create("ap_sqrtI", width, state, List(srca))
            case NodeType.sin     =>
                moduleEmitter.create("ap_sinI", width, state, List(srca))
            case NodeType.cos     =>
                moduleEmitter.create("ap_cosI", width, state, List(srca))
            case NodeType.tan     =>
                moduleEmitter.create("ap_tanI", width, state, List(srca))
            case _                    =>
                Error.raise("unsupported fixed point operation: " + node.op)
        }
        if (block.continuous) {
            moduleEmitter.addAssignment(s"$dest <= $expression;")
        } else {
            write(s"$dest <= $expression;")
        }
    }

    private def emitFloatOp(block: StateBlock, vt: FloatValueType,
                                    node: IRInstruction) {
        val dest = emitSymbol(node.dest)
        val srca = emitSymbol(node.srca)
        val srcb = emitSymbol(node.srcb)
        val state = block.label
        val width = vt.bits
        val expression = node.op match {
            case NodeType.abs => "{1'b0, " + srca + "[" + (width - 2) + ":0]}"
            case NodeType.neg =>
                "{~" + srca + "[" + (width - 1) + "], " +
                         srca + "[" + (width - 2) + ":0]}"
            case NodeType.add =>
                moduleEmitter.create("ap_addF" + width, width,
                                            state, List(srca, srcb))
            case NodeType.sub =>
                val negb = "{~" + srcb + "[" + (width - 1)  + "], " +
                              srcb + "[" + (width - 2) + ":0]}"
                moduleEmitter.create("ap_addF" + width, width,
                                            state, List(srca, negb))
            case NodeType.mul =>
                moduleEmitter.create("ap_mulF" + width, width,
                                            state, List(srca, srcb))
            case NodeType.div =>
                moduleEmitter.create("ap_divF" + width, width,
                                            state, List(srca, srcb))
            case NodeType.exp     =>
                moduleEmitter.create("ap_expF", width,
                                            state, List(srca))
            case NodeType.log     =>
                moduleEmitter.create("ap_logF", width,
                                            state, List(srca))
            case NodeType.sqrt =>
                moduleEmitter.create("ap_sqrtF" + width, width,
                                            state, List(srca))
            case NodeType.sin =>
                moduleEmitter.create("ap_sinF" + width, width, state,
                                     List(srca))
            case NodeType.cos =>
                moduleEmitter.create("ap_cosF" + width, width, state,
                                     List(srca))
            case NodeType.tan =>
                moduleEmitter.create("ap_tanF" + width, width, state,
                                     List(srca))
            case NodeType.convert =>
                moduleEmitter.create("ap_itof" + width, width,
                                     state, List(srca))
            case _ =>
                Error.raise("unsupported float operation: " + node.op)
        }
        if (block.continuous) {
            moduleEmitter.addAssignment(s"$dest <= $expression;")
        } else {
            write(s"$dest <= $expression;")
        }
    }

    private def emitInstruction(block: StateBlock, node: IRInstruction) {
        if (node.op == NodeType.avail) {
            emitAvailable(block, node)
        } else if (node.op == NodeType.assign) {
            emitAssign(block, node)
        } else {
            node.dest.valueType match {
                case it: IntegerValueType   => emitIntOp(block, it, node)
                case ft: FixedValueType     => emitFixedOp(block, ft, node)
                case ft: FloatValueType     => emitFloatOp(block, ft, node)
                case _                      =>
                    Error.raise("unsupported hardware type: " +
                                node.dest.valueType)
            }
        }
    }

    private def emitLoad(block: StateBlock, node: IRLoad) {
        val src = emitSymbol(node.src)
        val dest = emitSymbol(node.dest)
        val offset = emitSymbol(node.offset)
        if (node.src.valueType.flat) {

            // Load from a register.
            val maxOffset = node.src.valueType.bytes -
                            node.dest.valueType.bytes
            val builder = new Generator
            builder.write(s"case ($offset)")
            builder.enter
            for (i <- 0 to maxOffset) {
                val bottom = i * 8
                val top = bottom + node.dest.valueType.bits - 1
                builder.write(s"$i: $dest <= $src[$top:$bottom];")
            }
            builder.write(s"default: $dest <= 0;")
            builder.leave
            builder.write(s"endcase")
            if (block.continuous) {
                moduleEmitter.addAssignment(builder.getOutput)
            } else {
                write(builder)
            }

        } else {

            // Load from a RAM.
            // Note that this cannot be a continuous assignment.
            assert(!block.continuous)

            // Start the load.
            val wordBytes = ramWidth / 8
            val wordShift = (math.log(wordBytes) / math.log(2)).toInt
            val wordMask = (1 << wordShift) - 1
            val bits = node.dest.valueType.bits
            val wordCount = (bits + ramWidth - 1) / ramWidth
            val start = moduleEmitter.getRAMOffset(node.src)

            // The load is finished when the RAM is ready
            // and we have read all the requested words.
            addGuard(block, "ram_ready")
            addGuard(block, "(state == last_state)")
            addGuard(block, "!ram_re")
            if (bits > ramWidth) {
                addGuard(block, s"(ram_state == $wordCount)")
            }

            // Emit the code to start the load.
            val initBuilder = new Generator
            initBuilder.write(s"ram_state <= 0;")
            initBuilder.write(s"ram_addr <= $start + ($offset >> $wordShift);")
            initBuilder.write(s"ram_re <= 1;")
            initBuilder.write(s"ram_mask <= {$wordBytes{1'b1}};")
            addInitializer(initBuilder)

            // Emit the code to handle when the load (or part
            // of the load) completes.
            val updateBuilder = new Generator
            updateBuilder.write(s"if (ram_ready && " +
                                s"!ram_re && " +
                                s"state == last_state) begin")
            updateBuilder.enter
            if (bits > ramWidth) {
                // Multi-word load.
                // Note that this load will be aligned.
                updateBuilder.write(s"case (ram_state)")
                updateBuilder.enter
                for (word <- 0 until wordCount) {
                    val bottom = word * ramWidth
                    val width = math.min(ramWidth, bits - bottom)
                    val top = bottom + width - 1
                    updateBuilder.write(s"$word: $dest[$top:$bottom] <= " +
                                        s"ram_out[${width - 1}:0];")
                }
                updateBuilder.leave
                updateBuilder.write(s"endcase")
                updateBuilder.write(s"ram_addr <= ram_addr + 1;")
                updateBuilder.write(s"ram_state <= ram_state + 1;")
                updateBuilder.write(s"ram_re <= ram_state < ${wordCount - 1};")
            } else if (bits == ramWidth) {
                // Single-word load.
                updateBuilder.write(s"$dest <= ram_out;")
            } else {
                // Single-word load, but read less than a word.
                val maxOffset = wordBytes - node.dest.valueType.bytes
                updateBuilder.write(s"case ($offset & $wordMask)")
                updateBuilder.enter
                for (i <- 0 to maxOffset) {
                    val bottom = i * 8
                    val top = bottom + bits - 1
                    updateBuilder.write(s"$i: $dest <= ram_out[$top:$bottom];")
                }
                updateBuilder.write(s"default: $dest <= 0;")
                updateBuilder.leave
                updateBuilder.write(s"endcase")
            }
            updateBuilder.leave
            updateBuilder.write("end")
            addUpdater(updateBuilder)

        }
    }

    private def emitStore(block: StateBlock, node: IRStore) {
        val dest = emitSymbol(node.dest)
        val src = emitSymbol(node.src)
        val offset = emitSymbol(node.offset)
        if (node.dest.valueType.flat) {

            // Store to a register.
            val maxOffset = node.dest.valueType.bytes -
                            node.src.valueType.bytes
            val builder = new Generator
            builder.write(s"case ($offset)")
            builder.enter
            for (i <- 0 to maxOffset) {
                val bottom = i * 8
                val top = bottom + node.src.valueType.bits - 1
                builder.write(s"$i: $dest[$top:$bottom] <= $src;")
            }
            builder.leave
            builder.write(s"endcase")
            if (block.continuous) {
                moduleEmitter.addAssignment(builder.getOutput)
            } else {
                write(builder)
            }

        } else {

            // Store to a RAM.
            // Note that this cannot be a continuous assignment.
            assert(!block.continuous)

            val wordBytes = ramWidth / 8
            val wordShift = (math.log(wordBytes) / math.log(2)).toInt
            val wordMask = (1 << wordShift) - 1
            val bits = node.src.valueType.bits
            val wordCount = (bits + ramWidth - 1) / ramWidth
            val start = moduleEmitter.getRAMOffset(node.dest)

            // Store is done when the ram is ready and we have written
            // all of the words.
            addGuard(block, "ram_ready")
            addGuard(block, "!ram_we")
            addGuard(block, "(state == last_state)")
            if (bits > ramWidth) {
                addGuard(block, s"(ram_state == ${wordCount - 1})")
            }

            // Emit the code to run for the first word of the store.
            val initBuilder = new Generator
            initBuilder.write(s"ram_state <= 0;")
            initBuilder.write(s"ram_addr <= $start + ($offset >> $wordShift);")
            initBuilder.write(s"ram_we <= 1;")
            if (bits > ramWidth) {
                // First word of a multi-word store.
                // Note that this store will be aligned.
                val top = ramWidth - 1
                initBuilder.write(s"ram_in <= $src[$top:0];")
                initBuilder.write(s"ram_mask <= {$wordBytes{1'b1}};")
            } else if (bits == ramWidth) {
                // Single-word store.
                initBuilder.write(s"ram_in <= $src;")
                initBuilder.write(s"ram_mask <= {$wordBytes{1'b1}};")
            } else {
                // Single-word store, but store less than a word.
                val maxOffset = wordBytes - node.src.valueType.bytes
                initBuilder.write(s"case ($offset & $wordMask)")
                initBuilder.enter
                for (i <- 0 to maxOffset) {
                    val bottom = i * 8
                    val top = bottom + bits - 1
                    initBuilder.write(s"$i: begin")
                    initBuilder.enter
                    initBuilder.write(s"ram_in[$top:$bottom] " +
                                      s"<= $src[${bits - 1}:0];")
                    val mask = ((1 << (bits / 8)) - 1) << i
                    initBuilder.write(s"ram_mask <= $mask;")
                    initBuilder.leave
                    initBuilder.write(s"end")
                }
                initBuilder.leave
                initBuilder.write(s"endcase")
            }
            addInitializer(initBuilder)

            // Emit the code to handle the remainder of
            // a multi-word store.
            if (bits > ramWidth) {
                val updateBuilder = new Generator
                updateBuilder.write(s"if (ram_ready" +
                                    s" && !ram_we" +
                                    s" && state == last_state" +
                                    s" && ram_state < ${wordCount - 1}) begin")
                updateBuilder.enter
                updateBuilder.write(s"ram_we <= 1;")
                updateBuilder.write(s"ram_addr <= ram_addr + 1;")
                updateBuilder.write(s"ram_state <= ram_state + 1;")
                updateBuilder.write(s"case (ram_state)")
                updateBuilder.enter
                for (word <- 1 until wordCount) {
                    val bottom = word * ramWidth
                    val width = math.min(ramWidth, bits - bottom)
                    val top = bottom + width - 1
                    updateBuilder.write(s"$word: begin")
                    updateBuilder.enter
                    updateBuilder.write(s"ram_in[${width - 1}:0] <= " +
                                        s"$src[$top:$bottom];")
                    val mask = (1 << (width / 8)) - 1
                    updateBuilder.write(s"ram_mask <= $mask;")
                    updateBuilder.leave
                    updateBuilder.write(s"end")
                }
                updateBuilder.leave
                updateBuilder.write(s"endcase")
                updateBuilder.leave
                updateBuilder.write(s"end")
                addUpdater(updateBuilder)
            }

        }
    }

    private def emitConditional(block: StateBlock, node: IRConditional) {
        val testStr = emitSymbol(node.test)
        val tid = getNextState(graph, node.iTrue)
        val fid = getNextState(graph, node.iFalse)
        write(s"state <= $testStr ? $tid : $fid;")
    }

    private def emitSwitch(block: StateBlock, node: IRSwitch) {
        val testStr = emitSymbol(node.test)
        node.targets.foreach { case (s, b) =>
            if (s == null) {
                val next = getNextState(graph, b)
                write(s"state <= $next;")
            }
        }
        node.targets.foreach { case (s, b) =>
            if (s != null) {
                val condStr = emitSymbol(s)
                val next = getNextState(graph, b)
                write(s"if ($testStr == $condStr) begin")
                enter
                write(s"state <= $next;")
                leave
                write(s"end")
            }
        }
    }

    private def emitStart(block: StateBlock, node: IRStart) {
        val next = getNextState(graph, node.next)
        write(s"state <= $next;")
    }

    private def emitGoto(block: StateBlock, node: IRGoto) {
        if (!block.continuous) {
            val next = getNextState(graph, node.next)
            write(s"state <= $next;")
        }
    }

    private def emitCall(block: StateBlock, node: IRCall) {
        val dest = emitSymbol(node.dest)
        val width = node.dest.valueType.bits
        val state = block.label
        val args = node.args.map(emitSymbol)
        val result = moduleEmitter.create(node.func, width, state, args)
        write(s"$dest <= $result;")
    }

    private def emitPhi(block: StateBlock, node: IRPhi) {
        moduleEmitter.addPhi(node)
    }

    /** Add statements to execute on the first clock of the state. */
    protected def addInitializer(gen: Generator) {
        initializers += gen
    }

    /** Add statements to execute on every clock of the state. */
    protected def addUpdater(gen: Generator) {
        updaters += gen
    }

    def emit(block: StateBlock) {
        if (block.label > 0 && !block.continuous) {
            moduleEmitter.addState(block.label)
            emitBegin(block)
        }
        block.nodes.foreach { node =>
            write(s"// $node")
            node match {
                case inode: IRInstruction   => emitInstruction(block, inode)
                case ld:    IRLoad          => emitLoad(block, ld)
                case st:    IRStore         => emitStore(block, st)
                case stop:  IRStop          => emitStop(block, stop)
                case ret:   IRReturn        => emitReturn(block, ret)
                case gnode: IRGoto          => emitGoto(block, gnode)
                case cnode: IRConditional   => emitConditional(block, cnode)
                case snode: IRSwitch        => emitSwitch(block, snode)
                case nnode: IRNoOp          => ()
                case snode: IRStart         => emitStart(block, snode)
                case cnode: IRCall          => emitCall(block, cnode)
                case phi:   IRPhi           => emitPhi(block, phi)
                case _ => sys.error("internal: $node")
            }
        }
        if (block.label > 0) {
            if (!block.continuous) {
                emitEnd(block)
            }
        } else {
            write("last_state <= 0;")
            leave
            write("end else begin")
            enter
            write("last_state <= state;")
        }
    }

}
