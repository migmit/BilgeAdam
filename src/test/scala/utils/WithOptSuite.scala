package utils

case class OptTest(value: Int, state: Int) {
  def setValue(n: Int) = copy(value = n)
}

class WithOptSuite extends munit.FunSuite {
  import WithOpt._
  test("withOpt(None) changes nothing") {
    val optTest = OptTest(1, 2)
    assertEquals(optTest.withOpt[Int](None, _.setValue(_)), optTest)
  }
  test("withOpt(Some) sets the value") {
    val optTest = OptTest(1, 2)
    val newValue = 3
    assertEquals(
      optTest.withOpt(Some(newValue), _.setValue(_)),
      optTest.setValue(newValue)
    )
  }
}
