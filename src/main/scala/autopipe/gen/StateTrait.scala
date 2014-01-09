
package autopipe.gen

private[autopipe] trait StateTrait {

    var currentState = 0

    def nextState = {
        currentState += 1
        currentState
    }

}

