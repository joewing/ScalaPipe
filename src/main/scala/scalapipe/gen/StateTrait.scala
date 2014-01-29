package scalapipe.gen

private[scalapipe] trait StateTrait {

    var currentState = 0

    def nextState = {
        currentState += 1
        currentState
    }

}
