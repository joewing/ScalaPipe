package scalapipe.dsl

import scalapipe.Literal

class Config private[scalapipe](
        private[scalapipe] val name: Symbol,
        private[scalapipe] val default: Any)
