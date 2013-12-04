
package autopipe

import scala.collection.mutable.ListBuffer

private[autopipe] object ConstantFolder {

   def fold(co: CodeObject, root: ASTNode): ASTNode = {
      val folder = new ConstantFolder(co)
      folder.fold(root)
   }

}

private[autopipe] class ConstantFolder(co: CodeObject) {

   private def bool(v: Long): Boolean = v != 0

   private def bool(v: Double): Boolean = v != 0.0

   private def foldIntOp(node: ASTOpNode,
                         a: IntLiteral, b: IntLiteral): ASTNode = {
      node.op match {
         case NodeType.addr   => ASTOpNode(node.op, a)
         case NodeType.neg    => new IntLiteral(-a.value)
         case NodeType.compl  => new IntLiteral(~a.value)
         case NodeType.and    => new IntLiteral(a.value & b.value)
         case NodeType.or     => new IntLiteral(a.value | b.value)
         case NodeType.xor    => new IntLiteral(a.value ^ b.value)
         case NodeType.shr    => new IntLiteral(a.value >> b.value)
         case NodeType.shl    => new IntLiteral(a.value << b.value)
         case NodeType.add    => new IntLiteral(a.value + b.value)
         case NodeType.sub    => new IntLiteral(a.value - b.value)
         case NodeType.mul    => new IntLiteral(a.value * b.value)
         case NodeType.div    => new IntLiteral(a.value / b.value)
         case NodeType.mod    => new IntLiteral(a.value % b.value)
         case NodeType.not    => new IntLiteral(a.value == 0)
         case NodeType.land   => new IntLiteral(bool(a.value) && bool(b.value))
         case NodeType.lor    => new IntLiteral(bool(a.value) || bool(b.value))
         case NodeType.eq     => new IntLiteral(bool(a.value) == bool(b.value))
         case NodeType.ne     => new IntLiteral(bool(a.value) != bool(b.value))
         case NodeType.gt     => new IntLiteral(bool(a.value) >  bool(b.value))
         case NodeType.lt     => new IntLiteral(bool(a.value) <  bool(b.value))
         case NodeType.ge     => new IntLiteral(bool(a.value) >= bool(b.value))
         case NodeType.le     => new IntLiteral(bool(a.value) <= bool(b.value))
         case NodeType.abs    => new IntLiteral(math.abs(a.value))
         case NodeType.avail  => ASTOpNode(node.op, a)
         case _               => sys.error("internal")
      }
   }

   private def foldFloatOp(node: ASTOpNode,
                           a: FloatLiteral, b: FloatLiteral): ASTNode = {
      node.op match {
         case NodeType.addr   => ASTOpNode(node.op, a)
         case NodeType.neg    => new FloatLiteral(-a.value)
         case NodeType.add    => new FloatLiteral(a.value + b.value)
         case NodeType.sub    => new FloatLiteral(a.value - b.value)
         case NodeType.mul    => new FloatLiteral(a.value * b.value)
         case NodeType.div    => new FloatLiteral(a.value / b.value)
         case NodeType.mod    => new FloatLiteral(a.value % b.value)
         case NodeType.not    => new IntLiteral(!bool(a.value))
         case NodeType.land   => new IntLiteral(bool(a.value) && bool(b.value))
         case NodeType.lor    => new IntLiteral(bool(a.value) || bool(b.value))
         case NodeType.eq     => new IntLiteral(bool(a.value) == bool(b.value))
         case NodeType.ne     => new IntLiteral(bool(a.value) != bool(b.value))
         case NodeType.gt     => new IntLiteral(bool(a.value) >  bool(b.value))
         case NodeType.lt     => new IntLiteral(bool(a.value) <  bool(b.value))
         case NodeType.ge     => new IntLiteral(bool(a.value) >= bool(b.value))
         case NodeType.le     => new IntLiteral(bool(a.value) <= bool(b.value))
         case NodeType.abs    => new FloatLiteral(math.abs(a.value))
         case NodeType.exp    => new FloatLiteral(math.exp(a.value))
         case NodeType.log    => new FloatLiteral(math.log(a.value))
         case NodeType.sqrt   => new FloatLiteral(math.sqrt(a.value))
         case NodeType.sin    => new FloatLiteral(math.sin(a.value))
         case NodeType.cos    => new FloatLiteral(math.cos(a.value))
         case NodeType.tan    => new FloatLiteral(math.tan(a.value))
         case NodeType.avail  => ASTOpNode(node.op, a)
         case _               => sys.error("internal")
      }
   }

   private def foldOp(node: ASTOpNode): ASTNode = {

      val a = fold(node.a)
      val b = fold(node.b)

      val newNode = (a, b) match {
         case (ai: IntLiteral, bi: IntLiteral)     => foldIntOp(node, ai, bi)
         case (af: FloatLiteral, bf: FloatLiteral) => foldFloatOp(node, af, bf)
         case _                                    => ASTOpNode(node.op, a, b)
      }
      newNode.valueType = node.valueType
      newNode

   }

   private def foldAssign(node: ASTAssignNode): ASTNode =
      ASTAssignNode(fold(node.dest), fold(node.src))

   private def foldIf(node: ASTIfNode): ASTNode =
      ASTIfNode(fold(node.cond), fold(node.iTrue), fold(node.iFalse))

   private def foldSwitch(node: ASTSwitchNode): ASTNode = {
      val newNode = ASTSwitchNode(fold(node.cond))
      newNode.cases ++= node.cases.map { old =>
         (fold(old._1), fold(old._2))
      }
      newNode
   }

   private def foldWhile(node: ASTWhileNode): ASTNode =
      ASTWhileNode(fold(node.cond), fold(node.body))

   private def foldBlock(node: ASTBlockNode): ASTNode = {
      val children = new ListBuffer[ASTNode]
      node.children.foreach { children += fold(_) }
      ASTBlockNode(children)
   }

   private def foldCall(node: ASTCallNode): ASTNode = {
      val result = new ASTCallNode(node.func)
      val children = new ListBuffer[ASTNode]
      node.children.foreach { children += fold(_) }
      result.apply(children: _*)
      result.valueType = node.valueType
      result
   }

   private def foldConvert(node: ASTConvertNode): ASTNode = {
      val sub = fold(node.a)
      sub match {
         case l: Literal   => TypeConverter.convert(l, node.valueType)
         case _            => ASTConvertNode(sub, node.valueType)
      }
   }

   private def foldSymbol(node: ASTSymbolNode): ASTNode = {
      val result = ASTSymbolNode(node.symbol)
      if (node.index != null) {
         result.index = fold(node.index)
      }
      result.valueType = node.valueType
      result
   }

   private def foldReturn(node: ASTReturnNode): ASTNode = {
      ASTReturnNode(fold(node.a))
   }

   def fold(node: ASTNode): ASTNode = {
      val newNode = node match {
         case an: ASTAssignNode     => foldAssign(an)
         case in: ASTIfNode         => foldIf(in)
         case sw: ASTSwitchNode     => foldSwitch(sw)
         case wn: ASTWhileNode      => foldWhile(wn)
         case bn: ASTBlockNode      => foldBlock(bn)
         case cn: ASTCallNode       => foldCall(cn)
         case on: ASTOpNode         => foldOp(on)
         case cn: ASTConvertNode    => foldConvert(cn)
         case sn: ASTSymbolNode     => foldSymbol(sn)
         case rn: ASTReturnNode     => foldReturn(rn)
         case null                  => null
         case _                     => node
      }
      if (newNode != null) {
         newNode.fileName = node.fileName
         newNode.lineNumber = node.lineNumber
      }
      newNode
   }

}

