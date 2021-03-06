package redisseeker

import grails.test.*
import redisSeeker.RedisSeeker
import redis.seek.Entry
import redis.seek.Info;
import redis.clients.jedis.Jedis

class SeekerTests extends GrailsUnitTestCase {

  def seeker

  static INDEX_NAME = "items"
  static TEST_TAG = "tagged"

  protected void setUp() {
    super.setUp()
    seeker = new RedisSeeker(servers: ["127.0.0.1"])
    seeker.init()

    //index = seeker.seek.index(INDEX_NAME)

  }

  protected void tearDown() {
    super.tearDown()

    Jedis jedis = new Jedis("127.0.0.1")
    jedis.flushAll()
  }

  void insertItem(){
    insertItem("123")
  }

  void insertItem(id, tags=[TEST_TAG]){
    Entry entry = seeker.seek.add(id, System.currentTimeMillis() as Double)
    entry.shardBy "seller_id"
    entry.addField("seller_id", "2")
    entry.addField("status", "active")
    entry.addField("type", "normal")
    entry.addText("title", "titulin de prueba")
    tags.each {
	    entry.addTag(it)
	     }

//    entry.addOrder("start_time", System.currentTimeMillis() as Double)

    entry.save()

  }

  void testSimpleSearch() {
    insertItem()

    def items = seeker.list {
      //'index' "items"
      order "start_time"
      shard "2"
      field "status", "active"
    }

    assert items.find { it == "123" } != null

  }

  void assertThrowException(Closure c, klass){
    try{
      c()
      fail()
    } catch (e){
      assertTrue e.class == klass
    }
  }

//  void testSearchWithoutIndexShouldThrowException(){
//    insertItem()
//
//    assertThrowException({
//      def items = seeker.list {
//        order "start_time"
//        shard "seller_id", "2"
//        field "status", "active"
//      }
//    }, IllegalStateException)
//  }

  void testSearchWithoutOrderShouldThrowException(){
    insertItem()

    assertThrowException({
      def items = seeker.list {
        //'index' "items"
        shard "2"
        field "status", "active"
      }
    }, IllegalStateException)
  }

  void testSearchWithoutShardShouldThrowException(){
    insertItem()

    assertThrowException({
      def items = seeker.list {
        //'index' "items"
        order "start_time"
        field "status", "active"
      }
    }, IllegalStateException)
  }

  void testSearchWithoutFieldsShouldThrowException(){
    insertItem()

    assertThrowException({
      def items = seeker.list {
        //'index' "items"
        order "start_time"
        shard "2"
      }
    }, IllegalStateException)
  }

  void testSearchWithVariousFields(){
    insertItem()

    def items = seeker.list {
      //'index' "items"
      order "start_time"
      shard "2"
      field "status", "active"
      field "type", "normal"
    }

    assert items.find { it == "123" } != null
  }

  void testSearchWithTag(){
    insertItem()

    def items = seeker.list {
      //'index' "items"
      order "start_time"
      shard "2"
      field "status", "active"
      tag TEST_TAG
    }
    assert items.find { it == "123" } != null
  }

  void testFacets(){
    insertItem("123")
    insertItem("124",["tagged", "copado"])
    insertItem("125",["tagged", "copado"])

    Info<String, Info<?,?>> info = seeker.info {
      //'index' "items"
      shard "2"
    }
    
    println info
    assert info.total()==3
    assert info.status.total()==3
    assert info.status.active==3
    /** 
     * El total de tags no tiene mucho sentido porque muestra el acumulado de 
     *  todos los tags y una misma entrada puede tener más de un tag, eso haría 
     *  que se cuente 2 veces
     */
    assert info.tags.total()==5 

    assert info.tags.tagged==3
    assert info.tags.copado==2
  }

  void testSearchWithText(){
      insertItem()
      
      def items = seeker.list {
	  //'index' "items"
          order "start_time"
          shard "2"
          field "status", "active"
          text "title", "titulin"
          tag TEST_TAG
      }
      assert items.find { it == "123" } != null
  }
  
  void testSearchWithMultipleField(){
    insertItem()

    def items = seeker.list {
      //'index' "items"
      order "start_time"
      shard "2"
      field "status", "active", "closed"
      tag TEST_TAG
    }
    assert items.find { it == "123" } != null
  }

  void testSearchItemsWithDifferentOrders() {
    insertItem()
    insertItem("1234")

    def items = seeker.list {
      //'index' "items"
      order "start_time"
      shard "2"
      field "status", "active", "closed"
      tag TEST_TAG
    }

    assert items[0] == "123"
    assert items[1] == "1234"

    items = seeker.list {
//      'index' "items"
      order "start_time", "desc"
      shard "2"
      field "status", "active", "closed"
      tag TEST_TAG
    }

    assert items[1] == "123"
    assert items[0] == "1234"

  }

  void testSearchWithSimplePagination(){
    insertItem()
    insertItem("1234")

    def items = seeker.list {
//      'index' "items"
      order "start_time"
      shard "2"
      field "status", "active", "closed"
      tag TEST_TAG
      from 1
      to 1
    }

    assertEquals 2, items.result.totalCount
    assertEquals "1234", items[0]
    assertEquals 1, items.result.getIds().size()
  }

}
