# Jelastic
# Jelastic

实例化JElastic客户端组件:
<?xml version="1.0" encoding="UTF-8"?>
<beans xmlns="http://www.springframework.org/schema/beans"
       xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
       xsi:schemaLocation="http://www.springframework.org/schema/beans
	   http://www.springframework.org/schema/beans/spring-beans-3.0.xsd">

    <description>Elastic客户端</description>

    <bean id="elasticClient" class="com.plugin.elastic.search.ElasticFactoryBean">
        <property name="clusterName" value="plus_elastic" />
        <property name="autoCrateIndex" value="false" />
        <property name="discoveryType" value="zen" />
        <property name="discoveryZenMinMasterNodes" value="1" />
        <property name="discoveryZenPingTimeout" value="200" />
        <property name="discoveryInitialStateTimeout" value="500" />
        <property name="gatewayType" value="local" />
        <property name="indexNumberOfShards" value="1" />
        <property name="clusterRoutingSchedule" value="50" />
        <property name="serverAddress" value="127.0.0.1:9300" />
        <!--集群配置方式
            <property name="serverAddress" value="192.168.154.138:9300,192.168.154.139:9300" />
        -->
    </bean>

</beans>
测试Model:
public class VModel implements Serializable {

    @JElasticId
    private String id;

    @JElasticColumn
    private String pin;

    private String desction;

    @JElasticColumn(instore = false,analyzer = JEAnalyzer.not_analyzed)
    private String keyword;

    @JElasticColumn(instore = true)
    private Date created;


    public VModel() {
    }

    public VModel(String id, String pin, String desction, String keyword,Date date) {
        this.id = id;
        this.pin = pin;
        this.desction = desction;
        this.keyword = keyword;
        this.created = date;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setPin(String pin) {
        this.pin = pin;
    }

    public String getDesction() {
        return desction;
    }

    public void setDesction(String desction) {
        this.desction = desction;
    }

    public String getKeyword() {
        return keyword;
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }

    public Date getCreated() {
        return created;
    }

    public void setCreated(Date created) {
        this.created = created;
    }
}
说明:
    @JElasticId 定义存储在ES中的数据的documentId 值, 对应es的"_id"
    @JElasticColumn(instore = false,analyzer = JEAnalyzer.not_analyzed) 定义es mapping规则,索引写操作时会根据注解映射索引存储的mapping

测试用例：
public class TestElastic {


    ApplicationContext context = new ClassPathXmlApplicationContext("spring-bean-elastic.xml");

    TransportClient transportClient = null;

    JElasticRepository jElasticRepository = null;

    @Before
    public void init(){
        transportClient = (TransportClient) context.getBean("elasticClient");
        if(null != transportClient ){
           printf("init Elastic client finish!");
        }
        jElasticRepository = new JElasticRepository(transportClient);
        if(null != jElasticRepository ){
            printf("init Elastic Engine finish!");
        }
    }

    @Test
    public void createIndex(){
        VModel vmodel = new VModel("504","马云","电商、中国、创建","阿里巴巴222",new Date());
        printf("影响行数:" + jElasticRepository.indexCreate("test_index", "testName", vmodel));

    }

    @Test
    public void searchSuggest(){
        Set<String> fields = new HashSet<String>();
        fields.add("desction");
        fields.add("keyword");
        Collection<String> result = jElasticRepository.suggestSearch("test_index", "testName","中国",fields,5);
        printf("结果总数" + result.size());
        printf("推荐搜索" + result );
    }

    @Test
    public void search() throws IOException {
        Set<VModel> searhVm = new HashSet<VModel>();

        String indexName = "test_index";
        String typeName = "testName";
        String keyword = "中国";

        Set<String> fields = new HashSet<String>();
        fields.add("desction");
        fields.add("keyword");

        // 关键词转义
        QueryStringQueryBuilder queryBuilder = new QueryStringQueryBuilder(QueryParser.escape(keyword));
        for(String field : fields){
            queryBuilder.field(field);
        }
        SearchResponse searchResponse = jElasticRepository.search(indexName,typeName,null,queryBuilder, null,0,10,fields);

        for (SearchHit hit : searchResponse.getHits()) {
            //将文档中的每一个对象转换json串值
            String json = hit.getSourceAsString();
            //将json串值转换成对应的实体对象
            VModel vmodel = new ObjectMapper().readValue(json, VModel.class);
            vmodel.setDesction(jElasticRepository.getHighlightFields(hit,"desction"));
            vmodel.setKeyword(jElasticRepository.getHighlightFields(hit,"keyword"));
            searhVm.add(vmodel);
        }
        printf("[高亮]结果总数" + searhVm.size());
        printf("[高亮]推荐搜索" +jElasticRepository.convertObjectToJsonString(searhVm) );
    }


    @Test
    public void createBulkIndex(){
        Set<VModel> sets = new HashSet<VModel>();
        for(int i=0; i<3; i++){
            VModel plusPins = new VModel();
            plusPins.setId( (i+5) + "");
            plusPins.setPin("pin" + (i+5));
            plusPins.setKeyword("张三、李四"+i);
            plusPins.setDesction("测试数据");
            sets.add(plusPins);
        }
        printf("影响行数:" + jElasticRepository.indexCreate("test_index", "testName", sets));
    }

    @Test
    public void existIndex(){
        printf("" + jElasticRepository.indexExists("test_index"));
    }

    @Test
    public void getIndex(){
        printf(jElasticRepository.select("test_index","testName","502"));
    }

    @Test
    public void updateIndex(){
        VModel vmodel = new VModel();
        vmodel.setId("111");
        vmodel.setPin("dennisit2222");
        printf("影响行数:" + jElasticRepository.update("test_index", "testName", vmodel));
        printf(jElasticRepository.select("test_index","testName","111"));
    }

    @Test
    public void merge(){
        VModel vmodel = new VModel();
        vmodel.setId("11122");
        vmodel.setPin("dennisit666");
        printf("影响行数:" + jElasticRepository.merge("test_index", "testName", vmodel));
    }

    @Test
    public void multeGet(){
        Set<String> ids = new HashSet<String>();
        ids.add("5");
        ids.add("6");
        ids.add("7");
        printf("影响行数:" + jElasticRepository.select("test_index", "testName", ids));
    }

    @Test
    public void multeMerge(){
        Set<VModel> sets = new HashSet<VModel>();
        for(int i=0; i<3; i++){
            VModel plusPins = new VModel();
            plusPins.setId( (i+5) + "");
            plusPins.setPin("pin" + (2*i+5));
            sets.add(plusPins);
        }
        printf("影响行数:" + jElasticRepository.merge("test_index", "testName", sets));
    }

    /**
     {
     "test_index": {
         "properties": {
             "id": {
                 "type": "string",
                 "store": "yes"
             },
             "pin": {
                 "type": "string",
                 "store": "yes",
                 "index": "analyzed"
             },
             "keyword": {
                 "type": "string",
                 "index": "analyzed"
             },
             "created": {
                 "type": "date",
                 "store": "yes",
                 "index": "analyzed"
             }
         }
     }
     }
     */

    @Test
    public void testAnno(){
        XContentBuilder xContentBuilder = jElasticRepository.objectForMapping("test_index", new VModel());
        try {
            printf(xContentBuilder.string());
        } catch (IOException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
    }


    @Test
    public void delete(){
        printf(JSONUtils.toJSONString(jElasticRepository.delete("test_index","testName","X7upWFSYSPencBiLMZRFfw")));
    }

    public void printf(String text){
        System.out.println("[TEST]" + text);
    }
}

