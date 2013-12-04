

package autopipe

import scala.collection.mutable.ListBuffer

private[autopipe] object TypeChecker {

   private val primativeTypes = Array(
      ValueType.unsigned8,
      ValueType.signed8,
      ValueType.unsigned16,
      ValueType.signed16,
      ValueType.unsigned32,
      ValueType.signed32,
      ValueType.unsigned64,
      ValueType.signed64,
      ValueType.float32,
      ValueType.float64,
      ValueType.float96
   )

   def check(co: CodeObject, root: ASTNode): ASTNode = {
      val checker = new TypeChecker(co)
      checker.check(root)
   }

   def widestType(a: ValueType, b: ValueType): ValueType = {
      val aindex = primativeTypes.indexOf(a)
      val bindex = primativeTypes.indexOf(b)
      if (aindex >= 0 && bindex >= 0) {
         if (aindex > bindex) {
            return a
         } else {
            return b
         }
      } else if (a.isInstanceOf[PointerValueType] && bindex >= 0) {
         return a
      } else if (b.isInstanceOf[PointerValueType] && aindex >= 0) {
         return b
      } else if (a.isInstanceOf[FixedValueType] && bindex >= 0) {
         return a
      } else if (b.isInstanceOf[FixedValueType] && aindex >= 0) {
         return b
      } else {
         if (a != b) {
            return null
         } else {
            return a
         }
      }
   }

}

private[autopipe] class TypeChecker(co: CodeObject) {

   private def getWidestType(an: ASTNode, b: ValueType): ValueType = {
      val a = TypeChecker.widestType(getType(an), b)
      if (a != null) {
         return a
      } else {
         Error.raise("type mismatch: " + a + " vs " + b, an)
      }
   }

   private def getWidestType(an: ASTNode, bn: ASTNode): ValueType = {
      if (an.isInstanceOf[Literal] && !bn.isInstanceOf[Literal]) {
         getType(bn)
      } else if (!an.isInstanceOf[Literal] && bn.isInstanceOf[Literal]) {
         getType(an)
      } else {
         getWidestType(an, getType(bn))
      }
   }

   private def getOpType(node: ASTOpNode): ValueType = node.op match {
      case NodeType.addr   => ValueType.getPointer(getType(node.a))
      case NodeType.sizeof => ValueType.unsigned32
      case NodeType.neg    => getType(node.a)
      case NodeType.compl  => getType(node.a)
      case NodeType.and    => getWidestType(node.a, node.b)
      case NodeType.or     => getWidestType(node.a, node.b)
      case NodeType.xor    => getWidestType(node.a, node.b)
      case NodeType.shr    => getWidestType(node.a, node.b)
      case NodeType.shl    => getWidestType(node.a, node.b)
      case NodeType.add    => getWidestType(node.a, node.b)
      case NodeType.sub    => getWidestType(node.a, node.b)
      case NodeType.mul    => getWidestType(node.a, node.b)
      case NodeType.div    => getWidestType(node.a, node.b)
      case NodeType.mod    => getWidestType(node.a, node.b)
      case NodeType.not    => getType(node.a); ValueType.bool
      case NodeType.land   => getWidestType(node.a, node.b); ValueType.bool
      case NodeType.lor    => getWidestType(node.a, node.b); ValueType.bool
      case NodeType.eq     => getWidestType(node.a, node.b); ValueType.bool
      case NodeType.ne     => getWidestType(node.a, node.b); ValueType.bool
      case NodeType.gt     => getWidestType(node.a, node.b); ValueType.bool
      case NodeType.lt     => getWidestType(node.a, node.b); ValueType.bool
      case NodeType.ge     => getWidestType(node.a, node.b); ValueType.bool
      case NodeType.le     => getWidestType(node.a, node.b); ValueType.bool
      case NodeType.abs    => getType(node.a)
      case NodeType.exp    => getType(node.a)
      case NodeType.log    => getType(node.a)
      case NodeType.sqrt   => getType(node.a)
      case NodeType.avail  => getType(node.a); ValueType.unsigned32
      case _               => sys.error("internal: " + node)
   }

   def getType(node: ASTNode): ValueType = {

      def getSymbolType(sn: ASTSymbolNode): ValueType = {
         val stype = co.getType(sn)
         if (sn.index != null) {
            val lit: SymbolLiteral = sn.index match {
               case sl: SymbolLiteral => sl
               case _ => null
            }
            stype match {
               case at: ArrayValueType =>
                  at.itemType
               case st: StructValueType =>
                  if (lit != null) {
                     st.fields(lit.symbol)
                  } else {
                     Error.raise("invalid structure member", node)
                  }
               case ut: UnionValueType =>
                  if (lit != null) {
                     ut.fields(lit.symbol)
                  } else {
                     Error.raise("invalid union member", node)
                  }
               case nt: NativeValueType =>
                  if (lit != null) {
                     ValueType.any
                  } else {
                     Error.raise("invalid native structure member", node)
                  }
               case _ =>
                  Error.raise("not an array or structure", node)
            }
         } else {
            stype
         }
      }

      def get() = node match {
         case an: ASTAssignNode     => ValueType.void
         case wn: ASTWhileNode      => ValueType.void
         case in: ASTIfNode         => ValueType.void
         case sw: ASTSwitchNode     => ValueType.void
         case hn: ASTStopNode       => ValueType.void
         case bn: ASTBlockNode      => ValueType.void
         case op: ASTOpNode         => getOpType(op)
         case li: Literal           => li.valueType
         case an: ASTAvailableNode  => ValueType.bool
         case cn: ASTConvertNode    => cn.valueType
         case sn: ASTSymbolNode     => getSymbolType(sn)
         case cn: ASTCallNode       => cn.returnType
         case rn: ASTReturnNode     => ValueType.void
         case sp: ASTSpecial        => sp.valueType
         case _ => sys.error("internal: " + node)
      }

      if (node.valueType == ValueType.void) {
         node.valueType = get()
      }
      node.valueType
   }

   private def widen(an: ASTNode, b: ValueType): ASTNode = {
      val a = getType(an)
      if (a != b) {
         ASTConvertNode(check(an), b)
      } else {
         check(an)
      }
   }

   private def checkAssign(node: ASTAssignNode): ASTNode = {
      val dest = check(node.dest)
      val destType = getType(dest)
      val src = check(node.src)
      ASTAssignNode(dest, widen(src, destType))
   }

   private def checkCondition(node: ASTNode): ASTNode = {
      val cond = check(node)
      if (getType(cond) != ValueType.bool) {
         ASTConvertNode(cond, ValueType.bool)
      } else {
         cond
      }
   }

   private def checkIf(node: ASTIfNode): ASTNode = {
      val cond = checkCondition(node.cond)
      val iTrue = check(node.iTrue)
      val iFalse = check(node.iFalse)
      ASTIfNode(cond, iTrue, iFalse)
   }

   private def checkSwitch(node: ASTSwitchNode): ASTNode = {
      val cond = check(node.cond)
      val newNode = ASTSwitchNode(cond)
      newNode.cases ++= node.cases.map { old =>
         if (old._1 != null) {
            (widen(old._1, cond.valueType), check(old._2))
         } else {
            (null, check(old._2))
         }
      }
      newNode
   }

   private def checkWhile(node: ASTWhileNode): ASTNode = {
      val cond = checkCondition(node.cond)
      val body = check(node.body)
      ASTWhileNode(cond, body)
   }

   private def checkBlock(node: ASTBlockNode): ASTNode = {
      val children = new ListBuffer[ASTNode]
      node.children.foreach { children += check(_) }
      ASTBlockNode(children)
   }

   private def checkSymbol(node: ASTSymbolNode): ASTNode = {
      val result = ASTSymbolNode(node.symbol)
      if (node.index != null) {
         val temp = check(node.index)
         if (temp.valueType.isInstanceOf[IntegerValueType]) {
            result.index = temp
         } else if (node.index.isInstanceOf[SymbolLiteral]) {
            result.index = temp
         } else {
            result.index = ASTConvertNode(temp, ValueType.signed32)
         }
      }
      result
   }

   private def checkCall(node: ASTCallNode): ASTNode = {
      val result = ASTCallNode(node.func)
      val children = new ListBuffer[ASTNode]
      node.children.foreach { children += check(_) }
      result.apply(children: _*)
      result
   }

   private def checkSpecial(node: ASTSpecial): ASTNode = {
      val result = ASTSpecial(node.obj, node.method)
      result.args = node.children.map(check(_))
      result.valueType = node.valueType
      result
   }

   private def widen(op: ASTOpNode): ASTOpNode = {
      val a = check(op.a)
      val b = check(op.b)
      val widestType = getWidestType(a, b)
      val newa = widen(a, widestType)
      val newb = widen(b, widestType)
      ASTOpNode(op.op, newa, newb)
   }

   private def checkOp(node: ASTOpNode): ASTOpNode = node.op match {
      case NodeType.addr   => ASTOpNode(NodeType.addr, check(node.a))
      case NodeType.sizeof => ASTOpNode(NodeType.sizeof, check(node.a))
      case NodeType.neg    => ASTOpNode(NodeType.neg, check(node.a))
      case NodeType.compl  => ASTOpNode(NodeType.compl, check(node.a))
      case NodeType.and    => widen(node)
      case NodeType.or     => widen(node)
      case NodeType.xor    => widen(node)
      case NodeType.shr    => widen(node)
      case NodeType.shl    => widen(node)
      case NodeType.add    => widen(node)
      case NodeType.sub    => widen(node)
      case NodeType.mul    => widen(node)
      case NodeType.div    => widen(node)
      case NodeType.mod    => widen(node)
      case NodeType.not    => ASTOpNode(NodeType.not, check(node.a))
      case NodeType.land   => widen(node)
      case NodeType.lor    => widen(node)
      case NodeType.eq     => widen(node)
      case NodeType.ne     => widen(node)
      case NodeType.gt     => widen(node)
      case NodeType.lt     => widen(node)
      case NodeType.ge     => widen(node)
      case NodeType.le     => widen(node)
      case NodeType.abs    => ASTOpNode(NodeType.abs, check(node.a))
      case NodeType.exp    => ASTOpNode(NodeType.exp, check(node.a))
      case NodeType.log    => ASTOpNode(NodeType.log, check(node.a))
      case NodeType.sqrt   => ASTOpNode(NodeType.sqrt, check(node.a))
      case NodeType.avail  => ASTOpNode(NodeType.avail, check(node.a))
      case _               => sys.error("internal: " + node)
   }

   private def checkConvert(node: ASTConvertNode): ASTConvertNode = {
      val a = check(node.a)
      if (a.valueType.eq(ValueType.any)) {
         a.valueType = a match {
            case it: IntLiteral     => ValueType.signed64
            case ft: FloatLiteral   => ValueType.float64
            case _                  => node.valueType
         }
      }
      ASTConvertNode(a, node.valueType)
   }

   private def checkReturn(node: ASTReturnNode): ASTReturnNode = {
      ASTReturnNode(check(node.a))
   }

   def check(node: ASTNode): ASTNode = {
      val newNode = node match {
         case an: ASTAssignNode     => checkAssign(an)
         case in: ASTIfNode         => checkIf(in)
         case sw: ASTSwitchNode     => checkSwitch(sw)
         case wn: ASTWhileNode      => checkWhile(wn)
         case sn: ASTStopNode       => sn
         case bn: ASTBlockNode      => checkBlock(bn)
         case cn: ASTCallNode       => checkCall(cn)
         case sn: ASTSymbolNode     => checkSymbol(sn)
         case on: ASTOpNode         => checkOp(on)
         case cn: ASTConvertNode    => checkConvert(cn)
         case av: ASTAvailableNode  => av
         case rn: ASTReturnNode     => checkReturn(rn)
         case sp: ASTSpecial        => checkSpecial(sp)
         case null                  => null
         case li: Literal           => li
         case _                     => sys.error("internal: " + node)
      }
      if (newNode != null) {
         newNode.fileName = node.fileName
         newNode.lineNumber = node.lineNumber
         newNode.valueType = getType(newNode)
      }
      newNode
   }

}

