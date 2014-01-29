package scalapipe

private[scalapipe] trait IRContext {

    val kt: KernelType

    /** Determine if extra variables should be eliminated. */
    def eliminateVariables: Boolean

    /** Determine if two nodes share a resource. */
    def share(a: IRNode, b: IRNode): Boolean

    /** Determine if a state block shares a resource with a node. */
    final def share(a: StateBlock, b: IRNode): Boolean = a.nodes.exists { n =>
        share(n, b)
    }

}
