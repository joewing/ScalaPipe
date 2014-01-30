package scalapipe

import scala.collection.mutable.HashMap
import scala.collection.mutable.HashSet

private[scalapipe] object DependencySet {
    val Include = 0
    val Library = 1
    val IPath   = 2
    val LPath   = 3
}

private[scalapipe] class DependencySet {

    private val deps = new HashMap[Int, HashSet[String]]

    private def getOrCreate(t: Int): HashSet[String] = {
        deps.getOrElseUpdate(t, { new HashSet[String] })
    }

    def add(t: Int, n: String) {
        getOrCreate(t) += n
    }

    def add(o: DependencySet) {
        o.deps.foreach { t =>
            t._2.foreach { n => add(t._1, n) }
        }
    }

    def get(t: Int): List[String] = getOrCreate(t).toList

}
