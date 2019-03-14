package co.blocke.scalajack
package json.test.mapkeys

import org.scalatest.{ FunSpec, Matchers }
import java.util.UUID
import scala.reflect.runtime.universe.typeOf
import model.StringMatchHintModifier

class ClassPrimKeys() extends FunSpec with Matchers {

  val sj = ScalaJack()

  describe("-------------------------\n:  Class Map Key Tests  :\n-------------------------") {
    describe("+++ Positive Tests +++") {
      it("Simple (flat) class as key") {
        val a = SimpleClass("Larry", 32, true, "golf")
        val b = SimpleClass("Mike", 27, false, 125)
        val inst = SampleSimple(Map(a -> b))
        val js = sj.render(inst)
        assertResult("""{"m":{"{\"name\":\"Larry\",\"age\":32,\"isOk\":true,\"favorite\":\"golf\"}":{"name":"Mike","age":27,"isOk":false,"favorite":125}}}""") { js }
        assertResult(inst) {
          sj.read[SampleSimple](js)
        }
      }
      it("Complex class (having members that are classes) as key") {
        val a = SimpleClass("Larry", 32, true, "golf")
        val b = SimpleClass("Mike", 27, false, 125)
        val c1 = ComplexClass(UUID.fromString("1e6c2b31-4dfe-4bf6-a0a0-882caaff0e9c"), a, true)
        val c2 = ComplexClass(UUID.fromString("1e6c2b31-4dfe-4bf6-a0a0-882caaff0e9d"), b, false)
        val inst = SampleComplex(Map(c1 -> c2))
        val js = sj.render(inst)
        assertResult("""{"m":{"{\"id\":\"1e6c2b31-4dfe-4bf6-a0a0-882caaff0e9c\",\"simple\":{\"name\":\"Larry\",\"age\":32,\"isOk\":true,\"favorite\":\"golf\"},\"allDone\":true}":{"id":"1e6c2b31-4dfe-4bf6-a0a0-882caaff0e9d","simple":{"name":"Mike","age":27,"isOk":false,"favorite":125},"allDone":false}}}""") { js }
        assertResult(inst) {
          sj.read[SampleComplex](js)
        }
      }
      it("Simple (flat) trait as key") {
        val a: Pet = FishPet("Flipper", Food.Veggies, 74.33)
        val b: Pet = DogPet("Fido", Food.Meat, 3)
        val inst = SamplePet(Map(a -> b))
        val js = sj.render(inst)
        assertResult("""{"m":{"{\"_hint\":\"co.blocke.scalajack.json.test.mapkeys.FishPet\",\"name\":\"Flipper\",\"food\":\"Veggies\",\"waterTemp\":74.33}":{"_hint":"co.blocke.scalajack.json.test.mapkeys.DogPet","name":"Fido","food":"Meat","numLegs":3}}}""") { js }
        assertResult(inst) {
          sj.read[SamplePet](js)
        }
      }
      it("Complex trait (having members that are traits) as key") {
        val a: Pet = FishPet("Flipper", Food.Veggies, 74.33)
        val b: Pet = DogPet("Fido", Food.Meat, 3)
        val c: Pet = CompoundPet("Legion", Food.Pellets, b)
        val inst = SamplePet(Map(c -> a))
        val js = sj.render(inst)
        assertResult("""{"m":{"{\"_hint\":\"co.blocke.scalajack.json.test.mapkeys.CompoundPet\",\"name\":\"Legion\",\"food\":\"Pellets\",\"pet\":{\"_hint\":\"co.blocke.scalajack.json.test.mapkeys.DogPet\",\"name\":\"Fido\",\"food\":\"Meat\",\"numLegs\":3}}":{"_hint":"co.blocke.scalajack.json.test.mapkeys.FishPet","name":"Flipper","food":"Veggies","waterTemp":74.33}}}""") { js }
        assertResult(inst) {
          sj.read[SamplePet](js)
        }
      }
      it("Complex trait (having members that are traits) as key where trait member is null") {
        val a: Pet = FishPet("Flipper", Food.Veggies, 74.33)
        val b: Pet = null.asInstanceOf[Pet] // DogPet("Fido", Food.Meat, 3)
        val c: Pet = CompoundPet("Legion", Food.Pellets, b)
        val inst = SamplePet(Map(c -> a))
        val js = sj.render(inst)
        assertResult("""{"m":{"{\"_hint\":\"co.blocke.scalajack.json.test.mapkeys.CompoundPet\",\"name\":\"Legion\",\"food\":\"Pellets\",\"pet\":null}":{"_hint":"co.blocke.scalajack.json.test.mapkeys.FishPet","name":"Flipper","food":"Veggies","waterTemp":74.33}}}""") { js }
        assertResult(inst) {
          sj.read[SamplePet](js)
        }
      }
      it("Class having collections as members") {
        val a = PolyClass(Map("a" -> 1, "b" -> 2), List("one", "two"))
        val b = PolyClass(Map("x" -> 9, "y" -> 10), List("aye", "you"))
        val inst = SamplePolyClass(Map(a -> b))
        val js = sj.render(inst)
        assertResult("""{"m":{"{\"lookup\":{\"a\":1,\"b\":2},\"favs\":[\"one\",\"two\"]}":{"lookup":{"x":9,"y":10},"favs":["aye","you"]}}}""") { js }
        assertResult(inst) {
          sj.read[SamplePolyClass](js)
        }
      }
      it("Class having collections as members (empty collections") {
        val a = PolyClass(Map.empty[String, Int], List.empty[String])
        val b = PolyClass(Map.empty[String, Int], List.empty[String])
        val inst = SamplePolyClass(Map(a -> b))
        val js = sj.render(inst)
        assertResult("""{"m":{"{\"lookup\":{},\"favs\":[]}":{"lookup":{},"favs":[]}}}""") { js }
        assertResult(inst) {
          sj.read[SamplePolyClass](js)
        }
      }
      it("Custom trait hint field and value for key trait") {
        val petHintMod = StringMatchHintModifier(Map("BreathsWater" -> typeOf[FishPet], "BreathsAir" -> typeOf[DogPet]))
        val sj2 = ScalaJack()
          .withHints((typeOf[Pet] -> "kind"))
          .withHintModifiers((typeOf[Pet] -> petHintMod))

        val a: Pet = FishPet("Flipper", Food.Veggies, 74.33)
        val b: Pet = DogPet("Fido", Food.Meat, 3)
        val inst = SamplePet(Map(a -> b))
        val js = sj2.render(inst)
        assertResult("""{"m":{"{\"kind\":\"BreathsWater\",\"name\":\"Flipper\",\"food\":\"Veggies\",\"waterTemp\":74.33}":{"kind":"BreathsAir","name":"Fido","food":"Meat","numLegs":3}}}""") { js }
        assertResult(inst) {
          sj2.read[SamplePet](js)
        }
      }
      it("Custom trait hint field and value for key member's trait") {
        val petHintMod = StringMatchHintModifier(Map("BreathsWater" -> typeOf[FishPet], "BreathsAir" -> typeOf[DogPet]))
        val sj2 = ScalaJack()
          .withHints((typeOf[Pet] -> "kind"))
          .withHintModifiers((typeOf[Pet] -> petHintMod))

        val a: PetHolder = ShinyPetHolder("123 Main", FishPet("Flipper", Food.Veggies, 74.33))
        val b: PetHolder = ShinyPetHolder("210 North", DogPet("Fido", Food.Meat, 3))
        val inst = SampleShiny(Map(a -> b))
        val js = sj2.render(inst)
        assertResult("""{"m":{"{\"_hint\":\"co.blocke.scalajack.json.test.mapkeys.ShinyPetHolder\",\"address\":\"123 Main\",\"pet\":{\"kind\":\"BreathsWater\",\"name\":\"Flipper\",\"food\":\"Veggies\",\"waterTemp\":74.33}}":{"_hint":"co.blocke.scalajack.json.test.mapkeys.ShinyPetHolder","address":"210 North","pet":{"kind":"BreathsAir","name":"Fido","food":"Meat","numLegs":3}}}}""") { js }
        assertResult(inst) {
          sj2.read[SampleShiny](js)
        }
      }
      it("Key value is a class having a noncanoncial map") {
        val a = NCKey(Map(0 -> false, 1 -> true), "truth")
        val b = NCKey(Map(1 -> false, 0 -> true), "lie")
        val inst = SampleNCKey(Map(a -> b))
        val js = sj.render(inst)
        assertResult("""{"m":{"{\"nc\":{\"0\":false,\"1\":true},\"name\":\"truth\"}":{"nc":{"1":false,"0":true},"name":"lie"}}}""") { js }
        assertResult(inst) {
          sj.read[SampleNCKey](js)
        }
      }
      it("Extra/unneeded fields in key's JSON harmlessly ignored") {
        val js = """{"m":{"{\"name\":\"Larry\",\"bogus\":false,\"age\":32,\"isOk\":true,\"favorite\":\"golf\"}":{"name":"Mike","age":27,"isOk":false,"favorite":125}}}"""
        val a = SimpleClass("Larry", 32, true, "golf")
        val b = SimpleClass("Mike", 27, false, 125)
        val inst = SampleSimple(Map(a -> b))
        assertResult(inst) {
          sj.read[SampleSimple](js)
        }
      }
    }
    describe("--- Negative Tests ---") {
      it("Bad (invalid--missing field) class json as map key") {
        val js = """{"m":{"{\"age\":32,\"favorite\":\"golf\"}":{"name":"Mike","age":27,"isOk":false,"favorite":125}}}"""
        val msg = """[$.m.(map key)]: Class SimpleClass missing field name
                    |{"age":32,"favorite":"golf"}
                    |---------------------------^""".stripMargin
        the[co.blocke.scalajack.model.ReadMissingError] thrownBy sj.read[SampleSimple](js) should have message msg
      }
      it("Bad class json as map key (valid json, but wrong for given class)") {
        val js = """{"m":{"{\"name\":\"Larry\",\"age\":32,\"favorite\":\"golf\"}":{"name":"Mike","age":27,"isOk":false,"favorite":125}}}"""
        val msg = """[$.m.(map key)]: Class SimpleClass missing field isOk
                    |{"name":"Larry","age":32,"favorite":"golf"}
                    |------------------------------------------^""".stripMargin
        the[co.blocke.scalajack.model.ReadMissingError] thrownBy sj.read[SampleSimple](js) should have message msg
      }
      it("Bad json for member class") {
        val js = """{"m":{"{\"id\":\"1e6c2b31-4dfe-4bf6-a0a0-882caaff0e9c\",\"simple\":{\"name\":\"Larry\",\"isOk\":true,\"favorite\":\"golf\"},\"allDone\":true}":{"id":"1e6c2b31-4dfe-4bf6-a0a0-882caaff0e9d","simple":{"name":"Mike","age":27,"isOk":false,"favorite":125},"allDone":false}}}"""
        val msg = """[$.m.(map key).simple]: Class SimpleClass missing field age
                    |ple":{"name":"Larry","isOk":true,"favorite":"golf"},"allDone":true}
                    |--------------------------------------------------^""".stripMargin
        the[co.blocke.scalajack.model.ReadMissingError] thrownBy sj.read[SampleComplex](js) should have message msg
      }
      it("Bad (invalid) trait json as map key") {
        val js = """{"m":{"{\"_hint\":\"co.blocke.scalajack.json.test.mapkeys.FishPet\":\"Flipper\" \"food\":\"Veggies\",\"waterTemp\":74.33}":{"_hint":"co.blocke.scalajack.test.mapkeys.DogPet","name":"Fido","food":"Meat","numLegs":3}}}"""
        val msg = """[$.m.(map key)]: Expected comma here.
                    |t":"co.blocke.scalajack.json.test.mapkeys.FishPet":"Flipper" "food":"Veggies","waterTemp":74.33}
                    |--------------------------------------------------^""".stripMargin
        the[co.blocke.scalajack.model.ReadUnexpectedError] thrownBy sj.read[SamplePet](js) should have message msg
      }
      it("Bad trait json (missing hint) as map key") {
        val js = """{"m":{"{\"name\":\"Flipper\",\"food\":\"Veggies\",\"waterTemp\":74.33}":{"_hint":"co.blocke.scalajack.json.test.mapkeys.DogPet","name":"Fido","food":"Meat","numLegs":3}}}"""
        val msg = """[$.m.(map key)._hint]: Couldn't find expected type hint '_hint' for trait co.blocke.scalajack.json.test.mapkeys.Pet
                    |name":"Flipper","food":"Veggies","waterTemp":74.33}
                    |--------------------------------------------------^""".stripMargin
        the[co.blocke.scalajack.model.ReadInvalidError] thrownBy sj.read[SamplePet](js) should have message msg
      }
      it("Bad trait json (hint to unknown class) as map key") {
        val js = """{"m":{"{\"_hint\":\"co.blocke.scalajack.json.test.mapkeys.Bogus\",\"name\":\"Flipper\",\"food\":\"Veggies\",\"waterTemp\":74.33}":{"_hint":"co.blocke.scalajack.json.test.mapkeys.DogPet","name":"Fido","food":"Meat","numLegs":3}}}"""
        val msg = """[$.m.(map key)]: Unable to find class named "co.blocke.scalajack.json.test.mapkeys.Bogus"
                    |hint":"co.blocke.scalajack.json.test.mapkeys.Bogus","name":"Flipper","food":"Veggies","waterTemp":74
                    |--------------------------------------------------^""".stripMargin
        the[co.blocke.scalajack.model.ReadMissingError] thrownBy sj.read[SamplePet](js) should have message msg
      }
      it("Bad (invalid) trait json for member trait") {
        val js = """{"m":{"{\"_hint\":\"co.blocke.scalajack.json.test.mapkeys.CompoundPet\",\"name\":\"Legion\",\"food\":\"Pellets\",\"pet\":{\"_hint\":\"co.blocke.scalajack.json.test.mapkeys.DogPet\",\"name\":\"Fido\",\"food\":\"Meat\",\"numLegs\":3}}":{"_hint":"co.blocke.scalajack.json.test.mapkeys.FishPet","name"}"Flipper","food":"Veggies","waterTemp":74.33}}}"""
        val msg = """[$.m.CompoundPet(Legion,Pellets,DogPet(Fido,Meat,3)).name]: Expected a colon here
                    |blocke.scalajack.json.test.mapkeys.FishPet","name"}"Flipper","food":"Veggies","waterTemp":74.33}}}
                    |--------------------------------------------------^""".stripMargin
        the[co.blocke.scalajack.model.ReadUnexpectedError] thrownBy sj.read[SamplePet](js) should have message msg
      }
      it("Bad trait json (missing hint) for member trait") {
        val js = """{"m":{"{\"_hint\":\"co.blocke.scalajack.json.test.mapkeys.CompoundPet\",\"name\":\"Legion\",\"food\":\"Pellets\",\"pet\":{\"_hint\":\"co.blocke.scalajack.json.test.mapkeys.DogPet\",\"name\":\"Fido\",\"food\":\"Meat\",\"numLegs\":3}}":{"name":"Flipper","food":"Veggies","waterTemp":74.33}}}"""
        val msg = """[$.m.CompoundPet(Legion,Pellets,DogPet(Fido,Meat,3))._hint]: Couldn't find expected type hint '_hint' for trait co.blocke.scalajack.json.test.mapkeys.Pet
                    |name":"Flipper","food":"Veggies","waterTemp":74.33}}}
                    |--------------------------------------------------^""".stripMargin
        the[co.blocke.scalajack.model.ReadInvalidError] thrownBy sj.read[SamplePet](js) should have message msg
      }
      it("Bad trait json (hint to unknown classs) for member trait") {
        val js = """{"m":{"{\"_hint\":\"co.blocke.scalajack.json.test.mapkeys.CompoundPet\",\"name\":\"Legion\",\"food\":\"Pellets\",\"pet\":{\"_hint\":\"co.blocke.scalajack.json.test.mapkeys.DogPet\",\"name\":\"Fido\",\"food\":\"Meat\",\"numLegs\":3}}":{"_hint":"co.blocke.scalajack.json.test.mapkeys.Bogus","name":"Flipper","food":"Veggies","waterTemp":74.33}}}"""
        val msg = """[$.m.CompoundPet(Legion,Pellets,DogPet(Fido,Meat,3))]: Unable to find class named "co.blocke.scalajack.json.test.mapkeys.Bogus"
                    |hint":"co.blocke.scalajack.json.test.mapkeys.Bogus","name":"Flipper","food":"Veggies","waterTemp":74
                    |--------------------------------------------------^""".stripMargin
        the[co.blocke.scalajack.model.ReadMissingError] thrownBy sj.read[SamplePet](js) should have message msg
      }
      it("Bad collection value in map key class having collections") {
        val js = """{"m":{"{\"lookup\":{\"a\":true,\"b\":2},\"favs\":[\"one\",\"two\"]}":{"lookup":{"x":9,"y":10},"favs":["aye","you"]}}}"""
        val msg = """[$.m.(map key).lookup.a]: Expected an Int but parsed True
                    |{"lookup":{"a":true,"b":2},"favs":["one","two"]}
                    |---------------^""".stripMargin
        the[co.blocke.scalajack.model.ReadUnexpectedError] thrownBy sj.read[SamplePolyClass](js) should have message msg
      }
      it("Bad custom hint value for map key trait (sort instead of kind") {
        val petHintMod = StringMatchHintModifier(Map("BreathsWater" -> typeOf[FishPet], "BreathsAir" -> typeOf[DogPet]))
        val sj2 = ScalaJack()
          .withHints((typeOf[Pet] -> "kind"))
          .withHintModifiers((typeOf[Pet] -> petHintMod))
        val js = """{"m":{"{\"_hint\":\"co.blocke.scalajack.json.test.mapkeys.ShinyPetHolder\",\"address\":\"123 Main\",\"pet\":{\"sort\":\"BreathsWater\",\"name\":\"Flipper\",\"food\":\"Veggies\",\"waterTemp\":74.33}}":{"_hint":"co.blocke.scalajack.json.test.mapkeys.ShinyPetHolder","address":"210 North","pet":{"kind":"BreathsAir","name":"Fido","food":"Meat","numLegs":3}}}}"""
        val msg = """[$.m.(map key).pet.kind]: Couldn't find expected type hint 'kind' for trait co.blocke.scalajack.json.test.mapkeys.Pet
                    |name":"Flipper","food":"Veggies","waterTemp":74.33}}
                    |--------------------------------------------------^""".stripMargin
        the[co.blocke.scalajack.model.ReadInvalidError] thrownBy sj2.read[SampleShiny](js) should have message msg
      }
      it("Bad class for hint in Map key (trait)") {
        val petHintMod = StringMatchHintModifier(Map("BreathsWater" -> typeOf[FishPet], "BreathsAir" -> typeOf[DogPet]))
        val sj2 = ScalaJack()
          .withHints((typeOf[Pet] -> "kind"))
          .withHintModifiers((typeOf[Pet] -> petHintMod))
        val js = """{"m":{"{\"_hint\":\"co.blocke.scalajack.test.mapkeys.Bogus\",\"address\":\"123 Main\",\"pet\":{\"kind\":\"BreathsLava\",\"name\":\"Flipper\",\"food\":\"Veggies\",\"waterTemp\":74.33}}":{"_hint":"co.blocke.scalajack.json.test.mapkeys.ShinyPetHolder","address":"210 North","pet":{"kind":"BreathsAir","name":"Fido","food":"Meat","numLegs":3}}}}"""
        val msg = """[$.m.(map key)]: Unable to find class named "co.blocke.scalajack.test.mapkeys.Bogus"
                    |{"_hint":"co.blocke.scalajack.test.mapkeys.Bogus","address":"123 Main","pet":{"kind":"BreathsLava"
                    |------------------------------------------------^""".stripMargin
        the[co.blocke.scalajack.model.ReadMissingError] thrownBy sj2.read[SampleShiny](js) should have message msg
      }
      it("Bad custom hint value for Map key member's trait") {
        val petHintMod = StringMatchHintModifier(Map("BreathsWater" -> typeOf[FishPet], "BreathsAir" -> typeOf[DogPet]))
        val sj2 = ScalaJack()
          .withHints((typeOf[Pet] -> "kind"))
          .withHintModifiers((typeOf[Pet] -> petHintMod))
        val js = """{"m":{"{\"_hint\":\"co.blocke.scalajack.json.test.mapkeys.ShinyPetHolder\",\"address\":\"123 Main\",\"pet\":{\"kind\":\"BreathsLava\",\"name\":\"Flipper\",\"food\":\"Veggies\",\"waterTemp\":74.33}}":{"_hint":"co.blocke.scalajack.json.test.mapkeys.ShinyPetHolder","address":"210 North","pet":{"kind":"BreathsAir","name":"Fido","food":"Meat","numLegs":3}}}}"""
        val msg = """[$.m.(map key).pet.kind]: Couldn't materialize class for trait co.blocke.scalajack.json.test.mapkeys.Pet using hint BreathsLava
                    |r","address":"123 Main","pet":{"kind":"BreathsLava","name":"Flipper","food":"Veggies","waterTemp":74
                    |--------------------------------------------------^""".stripMargin
        the[co.blocke.scalajack.model.ReadInvalidError] thrownBy sj2.read[SampleShiny](js) should have message msg
      }
    }
  }
}
