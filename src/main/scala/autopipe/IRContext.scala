
package autopipe

private[autopipe] trait IRContext {

   val co: CodeObject

   /** Determine if extra variables should be eliminated. */
   def eliminateVariables: Boolean

   /** Determine if two nodes share a resource. */
   def share(a: IRNode, b: IRNode): Boolean

   /** Determine if a state block shares a resource with a node. */
   final def share(a: StateBlock, b: IRNode): Boolean = a.nodes.exists { n =>
      share(n, b)
   }

}

