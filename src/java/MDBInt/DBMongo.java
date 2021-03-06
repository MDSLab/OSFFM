/**Copyright 2016, University of Messina.
*
*   Licensed under the Apache License, Version 2.0 (the "License");
*   you may not use this file except in compliance with the License.
*   You may obtain a copy of the License at
*
*       http://www.apache.org/licenses/LICENSE-2.0
*
*   Unless required by applicable law or agreed to in writing, software
*   distributed under the License is distributed on an "AS IS" BASIS,
*   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
*   See the License for the specific language governing permissions and
*   limitations under the License.
*/
package MDBInt;

import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.MongoClient;
import com.mongodb.MongoCredential;
import com.mongodb.ServerAddress;
import com.mongodb.util.JSON;
import java.io.File;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import org.jdom2.Element;
import utils.*;
import MDBInt.MDBIException;
import org.json.JSONException;
import com.mongodb.AggregationOutput;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.logging.Level;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;


/**
 * @author agalletta
 * @author gtricomi
 */
public class DBMongo {

    private String serverURL;
    private String dbName;
    private String user;
    private String password;
    private int port;
    private MongoClient mongoClient;
    private HashMap map;
    private boolean connection;
    private Element serverList;
    private ParserXML parser;
    private MessageDigest messageDigest;
    static final Logger LOGGER = Logger.getLogger(DBMongo.class);
    private String identityDB="ManagementDB"; //DefaultVaue
    private static String configFile="/home/beacon/beaconConf/configuration_bigDataPlugin.xml";//"../webapps/OSFFM/WEB-INF/configuration_bigDataPlugin.xml";
    private String mdbIp;

    public String getMdbIp() {
        return mdbIp;
    }

    public void setMdbIp(String mdbIp) {
        this.mdbIp = mdbIp;
    }
    
    
    public String getDbName() {
        return dbName;
    }

    public void setDbName(String dbName) {
        this.dbName = dbName;
    }

    public String getIdentityDB() {
        return identityDB;
    }

    public void setIdentityDB(String identityDB) {
        this.identityDB = identityDB;
    }
   
    
    public DBMongo() {

        map = new HashMap();
        connection = false;
        try {
            messageDigest = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException ex) {
            ex.printStackTrace();
            System.exit(-1);
        }

    }

    public void init(String file) {
        Element params;
        try {
            parser = new ParserXML(new File(file));
            params = parser.getRootElement().getChild("pluginParams");
            dbName = params.getChildText("dbName");
            user = params.getChildText("user");
            password = params.getChildText("password");
            serverList = params.getChild("serversList");
            //this.connectReplication();

        } //init();
        catch (Exception ex) {
            ex.printStackTrace();
        }
    }
    
    public void init() {
        //String file=configFile;
        String file=System.getenv("HOME");
        if(file==null){
            file="/home/beacon/beaconConf/configuration_bigDataPlugin.xml";
            
            /*String restkey="[";
            String rest="["; 
            Object[] tk=System.getenv().keySet().toArray();
            Object[] t=System.getenv().values().toArray();
            for(int i=0;i<t.length;i++){
                restkey=restkey+";"+(String)tk[i];
                rest=rest+";"+(String)t[i];
            }
            rest=rest+"]";
            restkey=restkey+"]";
            LOGGER.error(restkey+"\n"+rest);*/
        }
        else
        {
            
            file="/home/beacon/beaconConf/configuration_bigDataPlugin.xml";
        }
        Element params;
        try {
            parser = new ParserXML(new File(file));
            params = parser.getRootElement().getChild("pluginParams");
            dbName = params.getChildText("dbName");
            user = params.getChildText("user");
            password = params.getChildText("password");
            //serverList = params.getChild("serversList");
            this.mdbIp = params.getChildText("serverip");
            

        } 
        catch (Exception ex) {
            ex.printStackTrace();
        }
    }
    /**
     * 
     * @param collection
     * @param resQuery
     * @param sortQuery
     * @return 
     * @author agalletta
     */
    private String conditionedResearch(DBCollection collection,BasicDBObject resQuery,BasicDBObject sortQuery) {
        try{
            DBCursor b= collection.find(resQuery).sort(sortQuery).limit(1);
            return b.next().toString();
        }catch(Exception e){
            LOGGER.error("Conditioned Research for collection: "+collection+", resQuery "+resQuery+", sortQuery "+sortQuery);
            return null;
        }
    }
    
     /**
     * 
     * @param collection
     * @param resQuery
     * @param sortQuery
     * @return 
     * @author agalletta
     */
    private DBObject conditionedResearch_Obj(DBCollection collection,BasicDBObject resQuery,BasicDBObject sortQuery) {
        try{
            DBCursor b= collection.find(resQuery).sort(sortQuery).limit(1);
            return b.next();
        }catch(Exception e){
            LOGGER.error("Conditioned Research for collection: "+collection+", resQuery "+resQuery+", sortQuery "+sortQuery);
            return null;
        }
    }
    private String conditionedResearch(DBCollection collection, BasicDBObject resQuery ,BasicDBObject sortQuery, BasicDBObject field) {
        try{
            JSONObject jsonized_field = new JSONObject(JSON.serialize(field));
            Iterator<?> keys = jsonized_field.keys();
            DBCursor b= collection.find(resQuery, field).sort(sortQuery).limit(1); //RITORNA IL FIELD SELEZIONATO

            return b.next().get((String)keys.next()).toString();

        }catch(Exception e){
            LOGGER.error("Conditioned Research for collection: "+collection+", resQuery "+resQuery+", sortQuery "+sortQuery);
            return null;
        }
    }
    
    //<editor-fold defaultstate="collapsed" desc="FAInfo Management Functions">
    /**
     * 
     * @param tenantName
     * @param faSite
     * @param faIP
     * @param faPort
     * @return 
     * @author gtricomi
     */
    public String getFAInfo(String tenantName, String faSite) {
        DB database = this.getDB(tenantName);
        DBCollection collection = database.getCollection("faInfo");
        BasicDBObject first = new BasicDBObject();
        first.put("SiteName", faSite);
        DBObject obj = null;
        obj = collection.findOne(first);
        if (obj==null)
            return null;
        return obj.toString();
    }
    /**
     * 
     * @param tenantName
     * @param faSite
     * @param faIP
     * @param faPort 
     * @author gtricomi
     */
    public void insertFAInfo(String tenantName, String faSite,String faIP,String faPort) {

        DB dataBase = this.getDB(tenantName);
        DBCollection collezione = this.getCollection(dataBase, "faInfo");
        BasicDBObject obj = new BasicDBObject();
        obj.append("SiteName", faSite);
        obj.append("Ip", faIP);
        obj.append("Port", faPort);
        collezione.save(obj);
    }
    
    //</editor-fold>
//<editor-fold defaultstate="collapsed" desc="NetTables Management Functions">
    /**
     * 
     * @param dbName
     * @param faSite, this is the cloud Id
     * @return 
     * @author gtricomi
     */
    public String getNetTables(String dbName, String faSite) {

        DB database = this.getDB(dbName);
        DBCollection collection = database.getCollection("netTables");
        
        BasicDBObject resQuery=new BasicDBObject("referenceSite",faSite);
        BasicDBObject sortQuery=new BasicDBObject("version",-1);
        String result=this.conditionedResearch(collection,resQuery,sortQuery);
        return result;
    }
    /**
     * 
     * @param dbName
     * @param faSite
     * @param key
     * @param list
     * @return 
     * @author agalletta
     */
    public Boolean checkNetTables(String dbName, String faSite, String key, LinkedHashSet<String> list) {

        DB database = this.getDB(dbName);
        DBCollection collection = database.getCollection("netTables");
        //DBCollection collection = database.getCollection("testIn");

        BasicDBObject tableQuery, andQuery;
        BasicDBList and;
        double version;
        BasicDBObject resQuery = new BasicDBObject("referenceSite", faSite);
        BasicDBObject sortQuery = new BasicDBObject("version", -1);
        DBCursor b = collection.find(resQuery).sort(sortQuery).limit(1);
        Iterator<String> it;
        if (!b.hasNext()) {
            //System.out.println("tabella non trovata");
            return null;
        } else {
            version = (double) b.next().get("version");
            //System.out.println(version);

            //andQuery=new BasicDBObject("referenceSite",faSite);
            and= new BasicDBList();
            and.add(resQuery);
            and.add(new BasicDBObject("version", version));
            it = list.iterator();
            while (it.hasNext()) {
               // resQuery.append("table." + key, it.next());
                and.add(new BasicDBObject("table." + key, it.next()));
            }
            andQuery=new BasicDBObject("$and",and);
            //System.out.println("query:" + andQuery);
            b = collection.find(andQuery);
            if (!b.hasNext()) {
              //  System.out.println("tabella incompleta");
                return false;
            } else {
                return true;

            }

            
        }
    }
    
    /**
     * This update Federation User with element. Used by BNA
     * @param dbName
     * @param tableName
     * @param faSite, this is the cloud Id
     * @param docJSON 
     * @author gtricomi
     */
    public void insertNetTables(String dbName,String faSite, String docJSON,int version) {

        DB dataBase = this.getDB(dbName);
        DBCollection collezione = this.getCollection(dataBase, "netTables");
        BasicDBObject obj = (BasicDBObject) JSON.parse(docJSON);
        obj.append("referenceSite", faSite);
        obj.append("insertTimestamp", System.currentTimeMillis());
        obj.append("version", version);
        collezione.save(obj);
    }
    /**
     * This function save NetTable provided to BNA inside the collection netTables. Used by BNA
     * @param tenant
     * @param idcloud, this is the cloud Id
     * @param jsonTable
     * @return 
     */
    public boolean insertNetTable(String tenant,String idcloud,String jsonTable){
        DB dataBase = this.getDB(tenant);
        DBCollection collezione = this.getCollection(dataBase, "netTables");
        BasicDBObject obj = (BasicDBObject) JSON.parse(jsonTable);
        obj.append("referenceSite", idcloud);
        obj.append("insertTimestamp", System.currentTimeMillis());
        collezione.save(obj);
        //this.insert(tenant, "NetTablesInfo", jsonTable);
        return true;
    }
    /**
     * 
     * @param tenant
     * @param referenceSite
     * @param fednets
     * @param version
     * @return 
     */
    public boolean insertfednetsinSite(String tenant,String referenceSite, JSONObject fednets,int version){
        
        try {
            //String fednetlist;
            DB dataBase = this.getDB(tenant);
            DBCollection collezione = this.getCollection(dataBase, "fednetsinSite");
            BasicDBObject obj = new BasicDBObject();
            
            //fednetlist=fednets.get(referenceSite).toString();
            obj.put("referenceSite", referenceSite);
            obj.put("version", version);
            String a= ((JSONArray)fednets.get(referenceSite)).toString(0);
            obj.put("fednets", a);
            obj.put("insertTimestamp", System.currentTimeMillis());
            collezione.save(obj);
            //this.insert(tenant, "NetTablesInfo", jsonTable);
            
        } catch (JSONException ex) {
                        LOGGER.error("JSON error - extracting fedNets from JsonObject "+ex.getMessage());
        }
        return true;
    }
    
    //ALFO
    public void insertNetTables(String tenant, String jsonTable) throws MDBIException,JSONException{

        DBCollection collezione = null;
        try {

            DB dataBase = this.getDB(tenant);
            collezione = this.getCollection(dataBase, "BNANetSeg");
            System.out.println("DOPO COLLEZIONE:" + collezione.toString());

            if (collezione == null) {
                DBObject options;
                collezione = dataBase.createCollection("BNANetSeg", null);
            }
        }
        catch (Exception e) {
            //System.err.println("Errore creazione tabella MONGO: " + collezione.toString());
            throw new MDBIException("Error during creation table BNANetSeg");
        }
        try{
            BasicDBObject obj = (BasicDBObject) JSON.parse(jsonTable);
            obj.append("insertTimestamp", System.currentTimeMillis());
            collezione.save(obj);
        }             

           
            //this.insert(tenant, "NetTablesInfo", jsonTable);
            
         catch (Exception e) {
            //System.err.println("Errore creazione tabella MONGO: " + collezione.toString());
            throw new MDBIException("Error during insertion value in table BNANetSeg");
        }

    }
    public void insertTablesData(String uuid, String tenant, Integer v, String refSite, String fedNet) throws MDBIException, JSONException{

        DBCollection collezione = null;
        try {

            DB dataBase = this.getDB(tenant);
            collezione = this.getCollection(dataBase, "BNATableData");
            System.out.println("DOPO COLLEZIONE:" + collezione.toString());

            if (collezione == null) {
                DBObject options;
                collezione = dataBase.createCollection("BNATableData", null);
            }
        } catch (Exception e) {
            throw new MDBIException("Error during creation table BNATableData: " + collezione.toString());
            
        }
        try{
            BasicDBObject obj = new BasicDBObject("Fk", uuid);
            obj.append("referenceSite", refSite);
            obj.append("version", v);
            obj.append("fedNet", fedNet);

            obj.append("insertTimestamp", System.currentTimeMillis());
            collezione.save(obj);
        }
        catch(Exception e){
        throw new MDBIException("Error during insertion value in table BANTableData");
        }
            //this.insert(tenant, "NetTablesInfo", jsonTable);
           
        

    }
     //</editor-fold>
    
    //<editor-fold defaultstate="collapsed" desc="siteTables Management Functions">
    /**
     * Used by BNA
     * @param dbName
     * @param faSite, this is the cloud Id
     * @return 
     * @author gtricomi
     */
    public String getSiteTables(String dbName, String faSite) {

        DB database = this.getDB(dbName);
        DBCollection collection = database.getCollection("siteTables");
        
        BasicDBObject resQuery=new BasicDBObject("referenceSite",faSite);
        BasicDBObject sortQuery=new BasicDBObject("version",-1);
        return this.conditionedResearch(collection,resQuery,sortQuery);

    }
    
    /**
     * 
     * @param dbName
     * @param faSite, this is the cloud Id
     * @param version
     * @return
     * @author caromeo
     */
    public String getSiteTables(String dbName, String faSite, Integer version) {

        Object o = null;
        DB database = this.getDB(dbName);
        DBCollection collection = database.getCollection("siteTables");
        
        BasicDBObject resQuery=new BasicDBObject("referenceSite",faSite).append("version", version);

        DBCursor uuid = collection.find(resQuery);
        System.out.println("");
        
        if (!uuid.hasNext()) {
            return null;
        } else {
            o = uuid.next();
            BasicDBObject bdo = (BasicDBObject) o;
            //System.out.println(bdo.get("siteEntry"));
            return bdo.get("siteEntry").toString();
        }
    }
    

    /**
     * 
     * @param dbName
     * @param fedten
     * @param site
     * @param field
     * @param version
     * @return 
     * @author caromeo
     */
    public String getFednetsInSiteTablesFromFedTenant(String dbName, String site, String field, Integer version){
        
        DB database = this.getDB(dbName);
        DBCollection collection = database.getCollection("fednetsinSite");

        BasicDBObject resQuery = new BasicDBObject();
        
        if(version != null){
                
            List<BasicDBObject> obj = new ArrayList<BasicDBObject>();
            obj.add(new BasicDBObject("referenceSite", site));
            obj.add(new BasicDBObject("version", version));
        
            resQuery.put("$and", obj);
        }
        else
            resQuery = new BasicDBObject("referenceSite", site);
        
        BasicDBObject sortQuery = new BasicDBObject("version",-1);        
        BasicDBObject fieldObj = new BasicDBObject(field, 1);
        
        return this.conditionedResearch(collection,resQuery, sortQuery, fieldObj);
    }


    /**
     * 
     * @param dbName
     * @param site
     * @param field
     * @param version
     * @return 
     * @author caromeo
     */
    public String getSiteTablesFromFedTenant(String dbName, String site, String field, Integer version){
        
        DB database = this.getDB(dbName);
        DBCollection collection = database.getCollection("siteTables");

        BasicDBObject resQuery = new BasicDBObject();
        List<BasicDBObject> obj = new ArrayList<BasicDBObject>();
        obj.add(new BasicDBObject("entrySiteTab.name",site));
        obj.add(new BasicDBObject("referenceSite", site));
        
        if(version != null)
            obj.add(new BasicDBObject("version", version));
        
        resQuery.put("$and", obj);

        BasicDBObject sortQuery = new BasicDBObject("version",-1);        
        BasicDBObject fieldObj = new BasicDBObject(field, 1);
        
        return this.conditionedResearch(collection,resQuery, sortQuery, fieldObj);
    }

    
    /**
     * 
     * @param dbName
     * @param faSite
     * @param key
     * @param list
     * @return 
     * @author agalletta
     */
    public Boolean checkSiteTables(String dbName, String faSite, String key, LinkedHashSet<String> list) {

        DB database = this.getDB(dbName);
        DBCollection collection = database.getCollection("siteTables");
        //DBCollection collection = database.getCollection("testIn");

        BasicDBObject tableQuery, andQuery;
        BasicDBList and;
        double version;
        BasicDBObject resQuery = new BasicDBObject("referenceSite", faSite);
        BasicDBObject sortQuery = new BasicDBObject("version", -1);
        DBCursor b = collection.find(resQuery).sort(sortQuery).limit(1);
        Iterator<String> it;
        if (!b.hasNext()) {
           // System.out.println("tabella non trovata");
            return null;
        } else {
            version = (double) b.next().get("version");
           // System.out.println(version);

            //andQuery=new BasicDBObject("referenceSite",faSite);
            and= new BasicDBList();
            and.add(resQuery);
            and.add(new BasicDBObject("version", version));
            it = list.iterator();
            while (it.hasNext()) {
               // resQuery.append("table." + key, it.next());
                and.add(new BasicDBObject("table" + key, it.next()));
            }
            andQuery=new BasicDBObject("$and",and);
           // System.out.println("query:" + andQuery);
            b = collection.find(andQuery);
            if (!b.hasNext()) {
             //   System.out.println("tabella incompleta");
                return false;
            } else {
                return true;

            }

            
        }
    }
    
    /**
     * This update Federation User with element. Table used by BNA
     * @param dbName
     * @param site, this is the cloud Id
     * @author caromeo
     */
    //public void insertTenantTables(String dbName, String site, Integer version, String docJSON) {
    
    public String getFednetsInSite(String dbName, String site){
        DB database = this.getDB(dbName);
        DBCollection collection = database.getCollection("fednetsinSite");
        BasicDBObject resQuery=new BasicDBObject("referenceSite",site);
        BasicDBObject sortQuery=new BasicDBObject("version",-1);
        
        return this.conditionedResearch(collection,resQuery,sortQuery);
    }
    public String getFednetsInSite_array(String dbName, String site)throws JSONException{
        DB database = this.getDB(dbName);
        DBCollection collection = database.getCollection("fednetsinSite");
        BasicDBObject resQuery=new BasicDBObject("referenceSite",site);
        BasicDBObject sortQuery=new BasicDBObject("version",-1);
        
        String obj=this.conditionedResearch(collection,resQuery,sortQuery);
        try{
            JSONObject jo=new JSONObject(obj);
            return (String)jo.get("fednets");
        }
        catch(JSONException je){
            LOGGER.error("JSONException Occurred in retrieving fednetsInSite Array");
            throw je;
        }
    }
    
    
    /**
     * This update Federation User with element. Table used by BNA
     * @param dbName
     * @param faSite, this is the cloud Id
     * @param docJSON 
     * @author gtricomi, caromeo
     */    
    public void insertSiteTables(String dbName,String faSite, String docJSON, Integer version) {
        DB dataBase = this.getDB(dbName);
        DBCollection collezione = this.getCollection(dataBase, "siteTables");
        BasicDBObject obj=new BasicDBObject();
        BasicDBObject obj_inner = (BasicDBObject) JSON.parse(docJSON);
        obj.append("siteEntry", obj_inner);
        obj.append("referenceSite", faSite);
        obj.append("version", version);
        obj.append("insertTimestamp", System.currentTimeMillis());
        collezione.save(obj);
    }    
     //</editor-fold>
    //BEACON>>>> Valutare se mantenere questa parte di informazioni su MongoDB
    //<editor-fold defaultstate="collapsed" desc="TenantTables Management Functions">
    /**
     * Used by BNA
     * @param dbName
     * @param faSite, this is the cloud Id
     * @return 
     * @author gtricomi
     */
    public String getTenantTables(String dbName, String faSite) {

        DB database = this.getDB(dbName);
        DBCollection collection = database.getCollection("TenantTables");
        
        BasicDBObject resQuery=new BasicDBObject("referenceSite",faSite);
        BasicDBObject sortQuery=new BasicDBObject("version",-1);
        return this.conditionedResearch(collection,resQuery,sortQuery);

    }
    
    /**
     * Used by BB
     * @param dbName
     * @param faSite, this is the cloud Id
     * @return 
     * @author gtricomi
     */
    public String getTenantONETables(String dbName, String faSite) throws JSONException {

        DB database = this.getDB(dbName);
        DBCollection collection = database.getCollection("ONEtenantTab");
        DBObject res=collection.findOne();
        JSONObject jo=new JSONObject(res.toString());
        return ((JSONObject)jo.get("entryTenantTab")).toString();
    }
    
    public String createSiteONETables(String dbName, String faSite) throws JSONException {

        DB database = this.getDB(dbName);
        DBCollection collection = database.getCollection("ONEsiteTab");
        DBObject res=collection.findOne();
        JSONObject jo=new JSONObject(res.toString());
        JSONObject e1=((JSONObject)jo.get("siteEntry"));
        collection = database.getCollection("siteTables");
        BasicDBObject query=new BasicDBObject();
        query.append("referenceSite", faSite);
        query.append("siteEntry.name", faSite);
        DBCursor cu=collection.find(query).sort(new BasicDBObject("version",-1));
        if(cu.hasNext())
            jo=new JSONObject(cu.next().toString());
        JSONObject e2=((JSONObject)jo.get("siteEntry"));
        JSONArray ja=new JSONArray();
        ja.put(e1);
        ja.put(e2);
        return ja.toString();
    }
    
    
    public String getNetONETables(String dbName, String faSite) throws JSONException {

        DB database = this.getDB(dbName);
        DBCollection collection = database.getCollection("ONEnetTab");
        DBObject res=collection.findOne();
        JSONObject jo=new JSONObject(res.toString());
        return ((JSONObject)jo.get("entryNetTab")).toString();
    }
    
    
    public String createNetONETables(String dbName, String faSite,int version) throws JSONException {
        JSONObject ftable=new JSONObject();
        ftable.put("version", version);
        JSONArray outja=new JSONArray();
        JSONArray inja=new JSONArray();
        try {
            DB database = this.getDB(dbName);
            DBCollection collection = database.getCollection("ONEnetTab");
            DBObject res = collection.findOne();
            JSONObject jo = new JSONObject(res.toString());
            inja.put((JSONObject) jo.get("entryNetTab"));
            collection = database.getCollection("BNATableData");
            BasicDBObject query = new BasicDBObject();
            query.append("referenceSite", faSite);
            query.append("fedNet", "reviewPrivate");
            DBCursor cu = collection.find(query).sort(new BasicDBObject("version", -1));
            if (cu.hasNext()) {
                jo = new JSONObject(cu.next().toString());
            }
            String fk = jo.getString("Fk");
            collection = database.getCollection("BNANetSeg");
            query = new BasicDBObject();
            query.append("FK", fk);
            query.append("netEntry.site_name", faSite);
            cu = collection.find(query).sort(new BasicDBObject("version", -1));
            if (cu.hasNext()) {
                jo = new JSONObject(cu.next().toString());
            }
            inja.put((JSONObject) jo.get("netEntry"));
        } catch (JSONException e) {
            throw e;
        }
        outja.put(inja);
        ftable.put("table", outja);
        return ftable.toString();
    }
    
    /**
     * 
     * @param dbName
     * @param faSite, this is the cloud Id
     * @param version
     * @return 
     * @author caromeo
     */
    public String getTenantTables(String dbName, String faSite, Integer version) {

        Object o = null;
        DB database = this.getDB(dbName);
        DBCollection collection = database.getCollection("TenantTables");
        
        BasicDBObject resQuery=new BasicDBObject("referenceSite",faSite).append("version", version);

        DBCursor uuid = collection.find(resQuery);
        System.out.println("");
        if (!uuid.hasNext()) {
            return null;
        } else {
            o = uuid.next();
            BasicDBObject bdo = (BasicDBObject) o;
            return bdo.get("entryTenantTab").toString();
        }
    }
    
   
    
    /**
     * 
     * @param dbName
     * @param fedten
     * @param site
     * @param field
     * @param version
     * @return 
     * @author caromeo
     */
    public String getTenantTablesFromFedTenant(String dbName, String fedten, String site, String field, Integer version){
        
        DB database = this.getDB(dbName);
        DBCollection collection = database.getCollection("TenantTables");

        BasicDBObject resQuery = new BasicDBObject();
        List<BasicDBObject> obj = new ArrayList<BasicDBObject>();
        obj.add(new BasicDBObject("entryTenantTab.name",fedten));
        obj.add(new BasicDBObject("referenceSite", site));
        
        if(version != null)
            obj.add(new BasicDBObject("version", version));
        
        resQuery.put("$and", obj);

        BasicDBObject sortQuery = new BasicDBObject("version",-1);        
        BasicDBObject fieldObj = new BasicDBObject(field, 1);
        
        return this.conditionedResearch(collection,resQuery, sortQuery, fieldObj);
    }
    
    /**
     * This update Federation User with element. Used by BNA
     * @param dbName
     * @param faSite, this is the cloud Id
     * @param docJSON
     * @deprecated old unsed
     * @author gtricomi
     */
    public void insertTenantTables(String dbName,String faSite, String docJSON) {

        DB dataBase = this.getDB(dbName);
        DBCollection collezione = this.getCollection(dataBase, "TenantTables");
        BasicDBObject obj = (BasicDBObject) JSON.parse(docJSON);
        obj.append("referenceSite", faSite);
        obj.append("insertTimestamp", System.currentTimeMillis());
        collezione.save(obj);
    }

    
    /**
     * This update Federation User with element. Used by BNA
     * @param dbName
     * @param site, this is the cloud Id
     * @param version 
     * @param docJSON 
     * @author caromeo
     */
    public void insertTenantTables(String dbName, String site, Integer version, String docJSON) {
        DB dataBase = this.getDB(dbName);
        DBCollection collezione = this.getCollection(dataBase, "TenantTables");

        BasicDBObject obj = new BasicDBObject("entryTenantTab", (BasicDBObject) JSON.parse(docJSON));
        obj.append("referenceSite", site);
        obj.append("version", version);
        obj.append("insertTimestamp", System.currentTimeMillis());
        collezione.save(obj);
    }
    
    /**
     * This update Federation User with element. Used by BNM
     * @param dbName
     * @param docJSON 
     * @author gtricomi
     */
    public void insertTenantTables(String dbName, String docJSON) {

        DB dataBase = this.getDB(dbName);
        DBCollection collezione = this.getCollection(dataBase, "fedsdnTenant");
        try{
        BasicDBObject obj = (BasicDBObject) JSON.parse(docJSON);
        
        obj.append("insertTimestamp", System.currentTimeMillis());
        collezione.save(obj);
        }catch(Exception e){
            LOGGER.error("Exception occurred in insertTenantTables DBMongoFunction");
        }
    }
    /**
     * Used by BNM
     * @param dbName
     * @param faSite, this is the cloud Id
     * @return 
     * @author gtricomi
     */
    public String getTenantTables(String dbName) {

        DB database = this.getDB(dbName);
        DBCollection collection = database.getCollection("fedsdnTenant");
        
 //04/07/2017 gt: eliminare la sort query, questa collezione dovrebbe avere un solo elemento(nel caso in cui si gestisca solo il tenant di federazione, diversamente se si
 //gestiscono anche gli user allora si deve aggiungere una variabile tra i parametri della funzione da usare come filtro per la resQuery da usare nella conditionedResearch commentata.
        BasicDBObject sortQuery=new BasicDBObject("version",-1);
        try{
            DBCursor b= collection.find().sort(sortQuery).limit(1);
            return b.next().toString();
        }catch(Exception e){
            LOGGER.error("Conditioned Research for collection: "+collection+", sortQuery "+sortQuery);
            return null;
        }
        //return this.conditionedResearch(collection,resQuery,sortQuery);
    }
    /**
     * Used by BNM
     * @param dbName
     * @param faSite, this is the cloud Id
     * @return 
     * @author gtricomi
     */
    public String getfedsdnTenantid(String dbName,String tenName) {

        DB database = this.getDB(dbName);
        DBCollection collection = database.getCollection("fedsdnTenant");
        
 //04/07/2017 gt: eliminare la sort query, questa collezione dovrebbe avere un solo elemento(nel caso in cui si gestisca solo il tenant di federazione, diversamente se si
 //gestiscono anche gli user allora si deve aggiungere una variabile tra i parametri della funzione da usare come filtro per la resQuery da usare nella conditionedResearch commentata.
        BasicDBObject sortQuery=new BasicDBObject("version",-1);
        try{
            BasicDBObject qid=new BasicDBObject("tenantEntry.name",tenName);
            DBCursor b= collection.find(qid).sort(sortQuery).limit(1);
            DBObject ttt=b.next();
            Integer i=new Integer(((String)ttt.get("tenantID")));
            return i.toString();
        }catch(Exception e){
            LOGGER.error("Conditioned Research for collection: "+collection+", sortQuery "+sortQuery);
            return null;
        }
        //return this.conditionedResearch(collection,resQuery,sortQuery);
    }
    //</editor-fold>
    
    public String getRunTimeInfo(String dbName, String uuid) {

        BasicDBObject first = new BasicDBObject();
        first.put("phisicalResourceId", uuid);

        DB database = this.getDB(dbName);
        DBCollection collection = database.getCollection("runTimeInfo");
        DBObject obj = null;

        obj = collection.findOne(first);

        return obj.toString();

    }
    public String getRunTimeInfo(String dbName, String idcloud,String uuidTemplate,String stackname) {

        BasicDBObject first = new BasicDBObject();
        first.put("idCloud", idcloud);
        first.put("uuidTemplate",uuidTemplate);
        first.put("stackName",stackname);

        DB database = this.getDB(dbName);
        DBCollection collection = database.getCollection("runTimeInfo");
        try{
            DBCursor b= collection.find(first).sort(new BasicDBObject("insertTimestamp",-1)).limit(1);
            System.out.println("query: "+first);
            return b.next().toString();
        }catch(Exception e){
            LOGGER.error("Conditioned Research for collection: "+collection+", resQuery , sortQuery ");
            return null;
        }
    }

    public ArrayList<String> getRunTimeInfos(String dbName, String idcloud,String uuidTemplate,String stackname) {
        ArrayList<String> al=new ArrayList<String>();
        
        BasicDBObject first = new BasicDBObject();
        first.put("idCloud", idcloud);
        first.put("uuidTemplate",uuidTemplate);
        first.put("stackName",stackname);

        DB database = this.getDB(dbName);
        DBCollection collection = database.getCollection("runTimeInfo");
        try{
            DBCursor b= collection.find(first).sort(new BasicDBObject("insertTimestamp",-1)).limit(1);
            System.out.println("query: "+first);
            while(b.hasNext())
                al.add(b.next().toString());
            return al;
        }catch(Exception e){
            LOGGER.error("Conditioned Research for collection: "+collection+", resQuery , sortQuery ");
            return null;
        }
        
        

    }

    private void connectReplication() {

        MongoCredential credential;
        ArrayList<ServerAddress> lista = new ArrayList();
        List<Element> servers = serverList.getChildren();
        //  System.out.println("dentro connect");
        String ip;
        int porta;
        try {
            for (int s = 0; s < servers.size(); s++) {
                ip = servers.get(s).getChildText("serverUrl");
                porta = Integer.decode(servers.get(s).getChildText("port"));
                //    lista.add(new ServerAddress(servers.get(s).getChildText("serverUrl"),Integer.decode(servers.get(s).getChildText("port"))));
                lista.add(new ServerAddress(ip, porta));
            }

            credential = MongoCredential.createMongoCRCredential(user, dbName, password.toCharArray());
            mongoClient = new MongoClient(lista, Arrays.asList(credential));
            connection = true;
        } catch (Exception ex) {
            ex.printStackTrace();
        }

    }

    /**
     *
     * @param dbName
     * @param userName
     * @param password
     * @param cloudID
     * @return jsonObject that contains credential for a specified cloud or null
     */
    public String getFederatedCredential(String dbName, String userName, String password, String cloudID) {

        DB dataBase = this.getDB(dbName);
        DBCollection collezione = this.getCollection(dataBase, "credentials");
        DBObject federationUser = null;
        BasicDBObject query = new BasicDBObject("federationUser", userName);
        BasicDBList credList;
        Iterator it;
        BasicDBObject obj;
        query.append("federationPassword", this.toMd5(password));
           // System.out.println("password: "+this.toMd5(password));

        federationUser = collezione.findOne(query);

        if (federationUser == null) {
            return null;
        }
        credList = (BasicDBList) federationUser.get("crediantialList");

        it = credList.iterator();
        while (it.hasNext()) {
            obj = (BasicDBObject) it.next();
            if (obj.containsValue(cloudID)) {
                return obj.toString();
            }
        }
        return null;
    }

    /**
     * This use only token. It will be
     *
     * @param dbName
     * @param, this is an UUID generated from simple_IDM when a new
     * Federation user is added.
     * @param cloudID
     * @return
     * @author gtricomi
     */
    public String getFederatedCredential(String dbName, String token, String cloudID) {
        DB dataBase = this.getDB(dbName);
        DBCollection collezione = this.getCollection(dataBase, "credentials");
        DBObject federationUser = null;
        BasicDBObject query = new BasicDBObject("token", token);
        BasicDBList credList;
        Iterator it;
        BasicDBObject obj;

        federationUser = collezione.findOne(query);

        if (federationUser == null) {
            return null;
        }
        credList = (BasicDBList) federationUser.get("crediantialList");

        it = credList.iterator();
        while (it.hasNext()) {
            obj = (BasicDBObject) it.next();
            if (obj.containsValue(cloudID)) {
                return obj.toString();
            }
        }
        return null;
    }
    
    /**
     * Returns generic federation infoes.
     *
     * @param dbName
     * @param token, this is an internal token?
     * @return
     * @author gtricomi
     */
    public String getFederationCredential(String dbName, String token) {
        DB dataBase = this.getDB(dbName);
        DBCollection collezione = this.getCollection(dataBase, "credentials");
        DBObject federationUser = null;
        BasicDBObject query = new BasicDBObject("token", token);
        federationUser = collezione.findOne(query);

        if (federationUser == null) {
            return null;
        }
        BasicDBObject bo = new BasicDBObject();
        bo.append("federationUser", (String) federationUser.get("federationUser"));
        bo.append("federationPassword", (String) federationUser.get("federationPassword"));
        return bo.toString();
    }

    public void connectLocale() {

        try {
            mongoClient = new MongoClient("10.9.240.1");//("172.17.3.142");
        } catch (Exception ex) {
            ex.printStackTrace();
        }

    }

    /**
     * Function used for testing/prototype.
     *
     * @author gtricomi
     */
    public void connectLocale(String ip) {

        try {
            
            mongoClient = new MongoClient(ip);
        } catch (Exception ex) {
            ex.printStackTrace();
        }

    }

    private void close() {

        try {

            mongoClient.close();
            map = new HashMap();
            connection = false;
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
    //ALfo per test
    public DB getDB_(String name) {
        return this.getDB(name);
    }
    private DB getDB(String name) {

        DB database = null;
        database = (DB) map.get(name);

        if (database == null) {
            database = mongoClient.getDB(name);
            map.put(name, database);
        }

        return database;

    }
    /**
     * This update Federation User with element 
     * @param dbName
     * @param collectionName
     * @param docJSON 
     */
    public void insertUser(String dbName, String collectionName, String docJSON) {

        DB dataBase = this.getDB(dbName);
        DBCollection collezione = this.getCollection(dataBase, collectionName);
        BasicDBObject obj = (BasicDBObject) JSON.parse(docJSON);
        String userName;
        userName = obj.getString("federationUser");
        obj.append("_id", userName);
        obj.append("insertTimestamp", System.currentTimeMillis());
        collezione.save(obj);
    }

    public void insert(String dbName, String collectionName, String docJSON) {

        DB dataBase = this.getDB(dbName);
        DBCollection collezione = this.getCollection(dataBase, collectionName);
        BasicDBObject obj = (BasicDBObject) JSON.parse(docJSON);
        obj.append("insertTimestamp", System.currentTimeMillis());
        collezione.save(obj);
    }

    public DBCollection getCollection(DB nameDB, String nameCollection) {

        nameCollection = nameCollection.replaceAll("-", "__");

        return nameDB.getCollection(nameCollection);

    }

    public List getListDB() {

        return mongoClient.getDatabaseNames();

    }

    public String getUser(String dbName, String collectionName, String userName, String password) {

        DB dataBase = this.getDB(dbName);
        DBCollection collezione = this.getCollection(dataBase, collectionName);
        DBObject federationUser = null;
        BasicDBObject query = new BasicDBObject("federationUser", userName);

        query.append("federationPassword", password);
        federationUser = collezione.findOne(query);

        if (federationUser != null) {
            return federationUser.toString();
        }

        return null;
    }

    public void updateUser(String dbName, String collectionName, String docJSON) {

        DB dataBase = this.getDB(dbName);
        DBCollection collezione = this.getCollection(dataBase, collectionName);
        BasicDBObject obj = (BasicDBObject) JSON.parse(docJSON);
        String userName;
        userName = obj.getString("federationUser");
        obj.append("_id", userName);

        collezione.save(obj);
    }

    /**
     * function that returns all element in collection without
     * _id,credentialList, federationPassword
     *
     * @param dbName
     * @param collectionName
     */
    public void listFederatedUser(String dbName, String collectionName) {

        DBCursor cursore;
        DB dataBase;
        DBCollection collezione;
        BasicDBObject campi;
        Iterator<DBObject> it;
        dataBase = this.getDB(dbName);
        collezione = this.getCollection(dataBase, collectionName);
        campi = new BasicDBObject();
        campi.put("_id", 0);
        campi.put("crediantialList", 0);
        campi.put("federationPassword", 0);
        cursore = collezione.find(new BasicDBObject(), campi);
        it = cursore.iterator();

        while (it.hasNext()) {
            System.out.println(it.next());
        }
    }

    public void insertFederatedCloud(String dbName, String collectionName, String docJSON) {

        DB dataBase = this.getDB(dbName);
        DBCollection collezione = this.getCollection(dataBase, collectionName);
        BasicDBObject obj = (BasicDBObject) JSON.parse(docJSON);
        String userName;
        userName = obj.getString("cloudId");
        obj.append("_id", userName);
        obj.append("insertTimestamp", System.currentTimeMillis());
        collezione.save(obj);
    }

    public String getFederateCloud(String dbName, String collectionName, String cloudId) {

        DB dataBase = this.getDB(dbName);
        DBCollection collezione = this.getCollection(dataBase, collectionName);
        DBObject federationUser = null;
        BasicDBObject query = new BasicDBObject("cloudId", cloudId);

        federationUser = collezione.findOne(query);

        if (federationUser != null) {
            return federationUser.toString();
        }

        return null;
    }

    public String getObj(String dbName, String collName, String query) throws MDBIException {

        BasicDBObject constrains = null, results = null;
        DBCursor cursore;
        DB db = this.getDB(dbName);
        DBCollection coll = db.getCollection(collName);
        BasicDBObject obj = (BasicDBObject) JSON.parse(query);
        constrains = new BasicDBObject("_id", 0);
        constrains.put("insertTimestamp", 0);
        cursore = coll.find(obj, constrains);
        try {
            results = (BasicDBObject) cursore.next();
        } catch (NoSuchElementException e) {
            LOGGER.error("manifest non trovato!");
            throw new MDBIException("Manifest required is not found inside DB.");
        }
        return results.toString();
    }

    /**
     * Returns an ArraList<String> where each String is a JSONObject in String
     * version.
     *
     * @param tenant
     * @param geoShape
     * @return
     */
    public ArrayList<String> getDatacenters(String tenant, String geoShape) {
        DB database = this.getDB(tenant);
        DBCollection collection = database.getCollection("datacenters");

        BasicDBObject shape = (BasicDBObject) JSON.parse(geoShape);
        ArrayList<String> datacenters = new ArrayList();
        BasicDBObject geoJSON = (BasicDBObject) shape.get("geometry");
        BasicDBObject geometry = new BasicDBObject();
        BasicDBObject geoSpazialOperator = new BasicDBObject();
        BasicDBObject query = new BasicDBObject();
        BasicDBObject constrains = new BasicDBObject("_id", 0);
        Iterator<DBObject> it;
        DBCursor cursore;

        geometry.put("$geometry", geoJSON);
        geoSpazialOperator.put("$geoIntersects", geometry);
        query.put("geometry", geoSpazialOperator);
        cursore = collection.find(query, constrains);

        it = cursore.iterator();
        while (it.hasNext()) {
            datacenters.add(it.next().toString());

        }
        return datacenters;
    }

    public String getDatacenter(String tenant, String idCloud) throws MDBIException {
        DB database = this.getDB(tenant);
        DBCollection collection = database.getCollection("datacenters");

        BasicDBObject first = new BasicDBObject();
        first.put("cloudId", idCloud);

        DBObject obj = null;
        try {
            obj = collection.findOne(first);
        } catch (Exception e) {
            throw new MDBIException(e.getMessage());
        }
        return obj.toString();

    }
    
    /**
     * It returns OSFFM token assigned to federationUser.
     * @param tenant
     * @param user, this is federation UserName
     * @return
     * @throws MDBIException
     * @author gtricomi
     */
    public String getFederationToken(String tenant, String user) throws MDBIException {
        DB database = this.getDB(this.identityDB);
        DBCollection collection = database.getCollection("Federation_Credential");

        BasicDBObject first = new BasicDBObject();
        first.put("federationUser", user);

        DBObject obj = null;
        try {
            obj = collection.findOne(first);
        } catch (Exception e) {
            throw new MDBIException(e.getMessage());
        }
        return (String)obj.get("token");

    }

    /**
     * Function used to retrieve cloudId from cmp_endpoint
     *
     * @param federationUser
     * @param cmp_endpoint
     * @return
     * @author gtricomi
     */
    public String getDatacenterIDfrom_cmpEndpoint(String federationUser, String cmp_endpoint) throws MDBIException {
        DB database = this.getDB(federationUser);
        DBCollection collection = database.getCollection("datacenters");

        BasicDBObject first = new BasicDBObject();
        first.put("idmEndpoint", cmp_endpoint);

        DBObject obj = null;
        try {
            obj = collection.findOne(first);
        } catch (Exception e) {
            throw new MDBIException("An Exception is generated by OSFFM DB connector, when getDatacenterIDfrom_cmpEndpoint is launched with [federationUser:\" " + federationUser + "\",cmp_endpoint: " + cmp_endpoint + "\"]\n" + e.getMessage());
        }
        return ((String) obj.get("cloudId"));

    }

    /**
     * Returns an ArraList<String> where each String is a JSONObject in String
     * version.
     *
     * @param tenant
     * @param deviceId, this is the Vm Name
     * @return
     */
    public ArrayList<String> getportinfoes(String tenant, String deviceId) {
        DB database = this.getDB(tenant);
        DBCollection collection = database.getCollection("portInfo");

        DBCursor cursore;
        BasicDBObject campi;
        Iterator<DBObject> it;
        campi = new BasicDBObject();
        campi.put("deviceId", deviceId);
        cursore = collection.find(new BasicDBObject(), campi);
        it = cursore.iterator();
        ArrayList<String> pI = new ArrayList<String>();
        while (it.hasNext()) {
            pI.add(it.next().toString());

        }
        return pI;
    }

    public void insertStackInfo(String dbName, String docJSON) {

        this.insert(dbName, "stackInfo", docJSON);
    }

    public void insertResourceInfo(String dbName, String docJSON) {

        this.insert(dbName, "resourceInfo", docJSON);

    }

    public void insertPortInfo(String dbName, String docJSON) {

        this.insert(dbName, "portInfo", docJSON);

    }

    public void insertRuntimeInfo(String dbName, String docJSON) {

        this.insert(dbName, "runTimeInfo", docJSON);

    }

    public ArrayList<String> findResourceMate(String dbName, String uuid) {

        BasicDBObject first = new BasicDBObject();
        first.put("phisicalResourceId", uuid);

        DB database = this.getDB(dbName);
        DBCollection collection = database.getCollection("runTimeInfo");
        DBObject obj = null;
        BasicDBObject query = new BasicDBObject();
        DBCursor cursore = null;
        ArrayList<String> mates = new ArrayList();
        Iterator<DBObject> it;

        obj = collection.findOne(first);

        if (obj != null) {
            query.put("localResourceName", obj.get("localResourceName"));
            query.put("stackName", obj.get("stackName"));
            query.put("uuidTemplate", obj.get("uuidTemplate"));
            query.put("resourceName", obj.get("resourceName"));
            query.put("type", obj.get("type"));
            query.put("state", false);
            query.put("idCloud", new BasicDBObject("$ne", obj.get("idCloud")));

            System.out.println(query);
            cursore = collection.find(query);
            
        }
        if (cursore != null) {
            it = cursore.iterator();
            while (it.hasNext()) {
                mates.add(it.next().toString());

            }
        }
        return mates;

    }
    
    public String findResourceMate(String dbName, String uuid,String dcid) {

        BasicDBObject first = new BasicDBObject();
        first.put("phisicalResourceId", uuid);

        DB database = this.getDB(dbName);
        DBCollection collection = database.getCollection("runTimeInfo");
        DBObject obj = null;
        BasicDBObject query = new BasicDBObject();
        DBCursor cursore = null;
        //ArrayList<String> mates = new ArrayList();
        Iterator<DBObject> it;

        obj = collection.findOne(first);

        if (obj != null) {
            query.put("localResourceName", obj.get("localResourceName"));
            query.put("stackName", obj.get("stackName"));
            query.put("resourceName", obj.get("resourceName"));
            query.put("type", obj.get("type"));
            query.put("state", false);
            query.put("idCloud",dcid);

            //System.out.println(query);
            //obj = collection.findOne(query);
            System.out.println("MONGO QUERY "+query+ " in DB:"+dbName+" for UUID:"+uuid );
            DBCursor b=collection.find(query).sort(new BasicDBObject("insertTimestamp",-1)).limit(1);
            if(obj!=null)
                //return (String)obj.get("localResourceName");
                return b.next().toString();
            else
                return null;
        }
        /*if (cursore != null) {
            it = cursore.iterator();
            while (it.hasNext()) {
                mates.add(it.next().toString());

            }
        }
        return mates;*/
        return null;
    }

    public boolean updateStateRunTimeInfo(String dbName, String phisicalResourceId, boolean newState) {
        boolean result = true;
        BasicDBObject first = new BasicDBObject();
        first.put("phisicalResourceId", phisicalResourceId);

        DB database = this.getDB(dbName);
        DBCollection collection = database.getCollection("runTimeInfo");
        BasicDBObject obj = null;

        obj = (BasicDBObject) collection.findOne(first);
        obj.append("state", newState);
        collection.save(obj);
        return result;
    }
    
    public boolean updateStateRunTimeInfo(String dbName, String phisicalResourceId, boolean newState,int num_twins,String requid) {
        boolean result = true;
        BasicDBObject first = new BasicDBObject();
        first.put("phisicalResourceId", phisicalResourceId);

        DB database = this.getDB(dbName);
        DBCollection collection = database.getCollection("runTimeInfo");
        BasicDBObject obj = null;

        obj = (BasicDBObject) collection.findOne(first);
        obj.append("state", newState);
        obj.append("numOFtwins", num_twins);
        obj.append("requestUID",requid);
        collection.save(obj);
        return result;
    }
    
    private String toMd5(String original) {
        /*MessageDigest md;
        byte[] digest;
        StringBuffer sb;
        String hashed = null;
        try {
            md = MessageDigest.getInstance("MD5");
            md.update(original.getBytes());
            digest = md.digest();
            sb = new StringBuffer();
            for (byte b : digest) {
                sb.append(String.format("%02x", b & 0xff));
            }
            hashed = sb.toString();
        } catch (NoSuchAlgorithmException ex) {
            ex.printStackTrace();
        }
        return hashed;*/
        return utils.staticFunctionality.toMd5(original);
    }
//////////////////////////////////////////////////////////////////////////////////////7
    //funzioni da rivedere /eliminare

    /**
     * Returns ArrayList with all federatedUser registrated for the
     * federationUser
     *
     * @param dbName
     * @param collectionName
     * @param federationUserName
     * @return
     * @author gtricomi
     */
    public ArrayList<String> listFederatedUser(String dbName, String collectionName, String federationUserName) {

        DBCursor cursore;
        DB dataBase;
        DBCollection collezione;
        BasicDBObject campi;
        Iterator<DBObject> it;
        dataBase = this.getDB(dbName);
        collezione = this.getCollection(dataBase, collectionName);
        campi = new BasicDBObject();
        campi.put("federationUser", federationUserName);
        cursore = collezione.find(campi);
        it = cursore.iterator();
        ArrayList<String> als = new ArrayList();
        while (it.hasNext()) {
            als.add(it.next().get("crediantialList").toString());
        }
        return als;
    }

    public void insertTemplateInfo(String db, String id, String templateName, Float version, String user, String templateRef) {

        BasicDBObject obj;

        obj = new BasicDBObject();

        obj.append("id", id);
        obj.append("templateName", templateName);
        obj.append("version", version);
        obj.append("user", user);
        obj.append("templateRef", templateRef);

        this.insert(db, "templateInfo", obj.toString());
    }

    public ArrayList<String> listTemplates(String dbName) {
        
        DBCursor cursore;
        DB dataBase;
        DBCollection collezione;
        Iterator<DBObject> it;
        dataBase = this.getDB(dbName);
        collezione = dataBase.getCollection("templateInfo");
        cursore = collezione.find();
        it = cursore.iterator();
        ArrayList<String> templatesInfo = new ArrayList();
        while (it.hasNext()) {
            templatesInfo.add(it.next().toString());
        }
        return templatesInfo;
    }

    /**
     * Returns a specific template istance.
     *
     * @param dbName
     * @return
     * @author gtricomi
     */
    public String getTemplate(String dbName, String uuidManifest) {

        DB dataBase;
        DBCollection collezione;
        dataBase = this.getDB(dbName);
        collezione = dataBase.getCollection("templateInfo");

        BasicDBObject first = new BasicDBObject();
        first.put("id", uuidManifest);
        DBObject j = collezione.findOne(first);
        return j.toString();
    }

    //BEACON>>> Function added for preliminaryDEMO.
    /**
     * Returns generic federation infoes.
     * @param dbName
     * @param value
     * @param type
     * @return 
     */
    public String getFederationCredential(String dbName, String value, String type) {
        DB dataBase = this.getDB(dbName);
        DBCollection collezione = this.getCollection(dataBase, "credentials");
        DBObject federationUser = null;
        BasicDBObject query = new BasicDBObject(type, value);
        BasicDBList credList;
        Iterator it;
        BasicDBObject obj;
        //String result = "{";
        federationUser = collezione.findOne(query);

        if (federationUser == null) {
            return null;
        }
        BasicDBObject bo = new BasicDBObject();
        bo.append("federationUser", (String) federationUser.get("federationUser"));
        bo.append("federationPassword", (String) federationUser.get("federationPassword"));
        return bo.toString();//result;
    }
    
    /**
     * Returns generic federation infoes.
     * @param dbName
     * @param value
     * @param type
     * @return 
     */
    public JSONObject getFedsdnCredential(String dbName, String value, String type) throws JSONException{
        DB dataBase = this.getDB(dbName);
        DBCollection collezione = this.getCollection(dataBase, "fedsdnCredential");
        DBObject federationUser = null;
        BasicDBObject query = new BasicDBObject(type, value);
        BasicDBList credList;
        Iterator it;
        BasicDBObject obj;
        //String result = "{";
        federationUser = collezione.findOne(query);

        if (federationUser == null) {
            return null;
        }
        JSONObject bo = new JSONObject();
        bo.put("tenantName", (String) federationUser.get("tenantName"));
        bo.put("tenantPass", (String) federationUser.get("tenantPass"));
        return bo;
    }
    
    private Iterable<DBObject> operate(String dbName, String collectionName, String campoMatch, String valoreMatch, String campoOperazione, String nomeOperazione, String operation) {

        DB database = this.getDB(dbName);
        DBCollection collezione;
        DBObject match, fields, project, groupFields, group, sort;
        AggregationOutput output;
        Iterable<DBObject> cursor;
        List<DBObject> pipeline;

        collezione = database.getCollection(collectionName);
        match = new BasicDBObject("$match", new BasicDBObject(campoMatch, valoreMatch));
        fields = new BasicDBObject(campoOperazione, 1);
        fields.put("_id", 0);

        project = new BasicDBObject("$project", fields);

        groupFields = new BasicDBObject("_id", campoOperazione);
        groupFields.put(nomeOperazione, new BasicDBObject(operation, "$" + campoOperazione));

        group = new BasicDBObject("$group", groupFields);
        sort = new BasicDBObject("$sort", new BasicDBObject(campoOperazione, -1));
        pipeline = Arrays.asList(match, project, group, sort);
        output = collezione.aggregate(pipeline);
        cursor = output.results();
        return cursor;

    }

    public float getVersion(String dbName, String collectionName, String templateRef) {

        String v = "version";
        Iterable<DBObject> cursor = this.operate(dbName, collectionName, "templateRef", templateRef, v, v, "$max");
        Iterator<DBObject> it;
        DBObject result, query;
        DBObject risultato;
        float versione = 0;

        it = cursor.iterator();

        if (it.hasNext()) {
            result = it.next();
            versione = (float) ((double) result.get(v));
        } else {
            query = new BasicDBObject("id", templateRef);
            risultato = this.find(dbName, collectionName, query);
            if (risultato != null) {
                versione = 0.1F;
            }
        }
        System.out.println("VERSIONE:" + versione);
        return versione;
//controlla che il cursore nn sia vuoto, se vuoto controlla se è presente come versione 0.1
        //altrimenti restituisci 0.1

    }

    public DBObject find(String dbName, String collName, DBObject obj) {
        DB dataBase = this.getDB(dbName);
        DBCollection collezione = dataBase.getCollection(collName);
        // BasicDBObject obj = (BasicDBObject) JSON.parse(query);
        DBObject risultato = collezione.findOne(obj);

        return risultato;

    }

    public DBObject getDatacenterFromId(String dbName, String id) {
        DB dataBase = this.getDB(dbName);
        DBCollection collezione = dataBase.getCollection("datacenters");
        BasicDBObject obj = new BasicDBObject("cloudId", id);
        DBObject risultato = collezione.findOne(obj);

        return risultato;
    }
public String getMapInfo(String dbName, String uuidTemplate) {

        DB database = this.getDB(dbName);
        DBCollection collection = database.getCollection("runTimeInfo");
        DBObject obj = null, datacenter, punto, country, geoShape;
        BasicDBObject query = new BasicDBObject();
        DBCursor cursore = null;
        Set<String> attivi = new HashSet<String>();
        ArrayList<String> listCloud = new ArrayList();
        Iterator<DBObject> it;
        String idCloud, idmEndpoint, idRisorsa, nameRisorsa;
        boolean isPresent,linkAttivo;
        MapInfo map;
        Shape s;
        boolean state;
        Risorse r;
        int i, j;
        Collegamenti link;
        Object array[];

        query.put("uuidTemplate", uuidTemplate);
        //cursore = collection.find(query).sort(new BasicDBObject("insertTimestamp",-1)).limit(3);  //query utile nel caso in cui il template viene deployato + di una volta
        cursore = collection.find(query).sort(new BasicDBObject("insertTimestamp",-1)).limit(1);
        map = new MapInfo();

        if (cursore != null) {
            it = cursore.iterator();
            while (it.hasNext()) {
                //oggetto map info
                obj = it.next();
                idCloud = (String) obj.get("idCloud");
                isPresent = listCloud.contains(idCloud);
                if (!isPresent) {

                    datacenter = this.getDatacenterFromId(dbName, idCloud);
                    idmEndpoint = (String) datacenter.get("idmEndpoint");
                    state = (boolean) obj.get("state");

                    punto = (DBObject) datacenter.get("geometry");
                    country = this.getFirstCountry(dbName, punto);
                    geoShape = (DBObject) country.get("geometry");
                    System.out.println("geogeo" + geoShape);
                    s = new Shape(idCloud, idmEndpoint, geoShape.toString(), state);
                    if (state) {
                        attivi.add(idCloud);
                        idRisorsa = (String) obj.get("phisicalResourceId");
                        nameRisorsa = (String) obj.get("resourceName");
                        r = new Risorse(idRisorsa, nameRisorsa);
                        s.addResource(r);
                        listCloud.add(idCloud);
                    }
                    map.addShape(s);

                } else {
                    s = map.getShape(idCloud);
                    state = (boolean) obj.get("state");
                    if (state) {
                        s.setState(state);
                        attivi.add(idCloud);

                        idRisorsa = (String) obj.get("phisicalResourceId");
                        nameRisorsa = (String) obj.get("resourceName");
                        r = new Risorse(idRisorsa, nameRisorsa);
                        s.addResource(r);
                    }
                }
            }
            array = attivi.toArray();
            
            //da controllare //secondo me potrebbe dare dei problemi...
            for (i = 0; i < array.length; i++) {
                for (j = i + 1; j < array.length; j++) {
                    //verifico se il link e' attivo
                    linkAttivo=this.testLink(dbName, (String) array[i], (String) array[j]);
                    if(linkAttivo){
                        link = new Collegamenti((String) array[i], (String) array[j]);
                        map.addCollegamento(link);
                    }
                }
            }
            /*         array = attivi.toArray();    //forzatura creata prima della sistemazione fatta da Antonio
           linkAttivo=this.testLink(dbName, "CETIC", "UME");
                    if(linkAttivo){
                        link = new Collegamenti("CETIC", "UME");
                        map.addCollegamento(link);
                    }
            */
        }
        return map.toString();
    }


  public boolean testLink(String tenantName, String Cloud1, String Cloud2){
    
        boolean attivo=false;
        BasicDBList or;
        BasicDBObject query;
        DB database = this.getDB(tenantName);
        DBCollection collection = database.getCollection("link");
        DBObject result;
        try{
        or = new BasicDBList();
        or.add(new BasicDBObject("_id", Cloud1+"_"+Cloud2));
        or.add(new BasicDBObject("_id", Cloud2+"_"+Cloud1));
        query= new BasicDBObject("$or",or);
        result = collection.findOne(query);
        attivo=(boolean) result.get("status");
        }
        catch(Exception ex){
            
            return false;         
        
        
        }
        
        
        
        return attivo;
    
    }

    public DBObject getFirstCountry(String tenant, DBObject shape) {

        //{"geometry": {$geoIntersects: {$geometry: { "coordinates" : [  15.434675, 38.193164  ], "type" : "Point" }}}}
        DB database = this.getDB(tenant);
        DBCollection collection = database.getCollection("Countries");
        DBObject result;
        BasicDBObject geometry = new BasicDBObject("$geometry", shape);
        BasicDBObject geoSpazialOperator = new BasicDBObject("$geoIntersects", geometry);
        BasicDBObject query = new BasicDBObject("geometry", geoSpazialOperator);
        BasicDBObject constrains = new BasicDBObject("_id", 0);
        Iterator<DBObject> it;
        result = collection.findOne(query, constrains);
        return result;
    }

    public ArrayList getCountry(String tenant, DBObject shape) {

        //{"geometry": {$geoIntersects: {$geometry: { "coordinates" : [  15.434675, 38.193164  ], "type" : "Point" }}}}
        DB database = this.getDB(tenant);
        DBCollection collection = database.getCollection("Countries");
        ArrayList<String> datacenters = new ArrayList();

        BasicDBObject geometry = new BasicDBObject("$geometry", shape);
        BasicDBObject geoSpazialOperator = new BasicDBObject("$geoIntersects", geometry);
        BasicDBObject query = new BasicDBObject("geometry", geoSpazialOperator);
        BasicDBObject constrains = new BasicDBObject("_id", 0);
        Iterator<DBObject> it;
        DBCursor cursore;

        cursore = collection.find(query, constrains);

        it = cursore.iterator();
        while (it.hasNext()) {
            datacenters.add(it.next().toString());

        }

        return datacenters;
    }
   
//<editor-fold defaultstate="collapsed" desc="Federation Tenant Credential Management">
    /**
     * Method used for the retrieve Database name for Federation Tenant.
     * @param field
     * @param value
     * @return 
     */
    public String getTenantDBName(String field,String value){
       DB database = this.getDB(this.identityDB);
       DBCollection collection = database.getCollection("Federation_Credential");
       BasicDBObject researchField = new BasicDBObject(field, value);
       DBObject risultato = collection.findOne(researchField);
//06/07/2017 gt: il nome del tenant corrisponde al nome db quindi il campo da ricercare deve essere modificato in federationTenant       
       String tenantDbName=(String)risultato.get("federationTenant");//("dbname");
       return tenantDbName;
    }
    
    /**
     * Method used for the retrieve Database name for Federation Tenant from federated tenantuuid.
     * @param uuid
     * @param cloudid
     * @return 
     */
    public String getTenantDBNamefromfedtenuuid(String uuid,String cloudid){
       DB database = this.getDB(this.identityDB);
       DBCollection collection = database.getCollection("fedtenanttoBor");
       BasicDBObject researchField = new BasicDBObject("fedUuid", uuid);
       researchField.append("cloudID", cloudid);
       DBObject risultato = collection.findOne(researchField);
       String tenantDbName=(String)risultato.get("borrowerName");
       return tenantDbName;
    }
    
    /**
     * Method used for the retrieve  Federated Tenant UUID from Borrower and cmp_endpoint.
     * @param tenant
     * @param cmp_endpoint
     * @return tenantuuid
     */
    public String getTenantuuidfromborrower(String tenant,String cmp_endpoint){
       DB database = this.getDB(this.identityDB);
       DBCollection collection = database.getCollection("fedtenanttoBor");
       BasicDBObject researchField = new BasicDBObject("fedUuid", tenant);
       researchField.append("cmp_endpoint", cmp_endpoint);
       DBObject risultato = collection.findOne(researchField);
       String tenantuuid=(String)risultato.get("fedUuid");
       return tenantuuid;
    }
    
    public String getTenantName(String field,String value){
       DB database = this.getDB(this.identityDB);
       DBCollection collection = database.getCollection("Federation_Credential");
       BasicDBObject researchField = new BasicDBObject(field, value);
       DBObject risultato = collection.findOne(researchField);
       String tenantName=(String)risultato.get("federationTenant");
       return tenantName;
    }
    
    public boolean verifyTenantCredentials(String user, String password){
        DB database = this.getDB(this.identityDB);
        DBCollection collection = database.getCollection("Federation_Credential");
        BasicDBObject researchField = new BasicDBObject("federationTenant", user);
       
        DBObject risultato = collection.findOne(researchField);
        try{
            if ( ((String)risultato.get("password")).equals(password) ) return true;
            else return false;
        }catch(Exception ex){
            return false;    
        }
    }
    
    public String getTenantToken(String field,String value){
       DB database = this.getDB(this.identityDB);
       DBCollection collection = database.getCollection("Federation_Credential");
       BasicDBObject researchField = new BasicDBObject(field, value);
       DBObject risultato = collection.findOne(researchField);
       String tenantName=(String)risultato.get("token");
       return tenantName;
    }
    
    public String getTenantToken(String field,String value, String tenant){
       DB database = this.getDB(tenant);
       DBCollection collection = database.getCollection("Federation_Credential");
       BasicDBObject researchField = new BasicDBObject(field, value);
       DBObject risultato = collection.findOne(researchField);
       String tenantName=(String)risultato.get("token");
       return tenantName;
    }
    /**
     * This function provides the endpoint of the component used inside the architecture.
     * @param field
     * @param value
     * @return 
     */
    public String getInfo_Endpoint(String field,String value){
       DB database = this.getDB(this.identityDB);
       DBCollection collection = database.getCollection("SystemInfos");
       BasicDBObject researchField = new BasicDBObject(field, value);
       DBObject risultato = collection.findOne(researchField);
       String valueSearched=(String)risultato.get("endpoint");
       return valueSearched;
    }
    
    public void insertfedsdnSite(String json){
        
        DB dataBase = this.getDB(this.identityDB);
        DBCollection collezione = dataBase.getCollection("fedsdnSite");
        BasicDBObject obj = (BasicDBObject) JSON.parse(json);
        obj.append("insertTimestamp", System.currentTimeMillis());
        collezione.save(obj);
    }
    
    public String getfedsdnSite(String name){
       DB database = this.getDB(this.identityDB);
       DBCollection collection = database.getCollection("fedsdnSite");
       BasicDBObject researchField = new BasicDBObject("name", name);
       DBObject risultato = collection.findOne(researchField);
       return risultato.toString();
    }
     public int getfedsdnSiteID(String name){
       DB database = this.getDB(this.identityDB);
       DBCollection collection = database.getCollection("fedsdnSite");
       BasicDBObject researchField = new BasicDBObject("name", name);
       DBObject risultato = collection.findOne(researchField);
       
       return ((Number) risultato.get("id")).intValue();//((Number) mapObj.get("autostart")).intValue()//(float) ((double) result.get(v))
    }

    public void insertfedsdnSite(String json, String tenant){
        
        DB dataBase = this.getDB(tenant);
        DBCollection collezione = dataBase.getCollection("fedsdnSite");
        BasicDBObject obj = (BasicDBObject) JSON.parse(json);
        obj.append("insertTimestamp", System.currentTimeMillis());
        collezione.save(obj);
    }
    
    public String getfedsdnSite(String name, String tenant){
       DB database = this.getDB(tenant);
       DBCollection collection = database.getCollection("fedsdnSite");
       BasicDBObject researchField = new BasicDBObject("name", name);
       DBObject risultato = collection.findOne(researchField);
       return risultato.toString();
    }
    public String getfedsdnSite(int fedsdnSiteID, String tenant)throws JSONException{
       DB database = this.getDB(tenant);
       DBCollection collection = database.getCollection("fedsdnSite");
       BasicDBObject researchField = new BasicDBObject("siteID", fedsdnSiteID);
       DBObject risultato_tmp = collection.findOne(researchField);
       JSONObject risultato = new JSONObject((String)risultato_tmp.get("siteEntry"));
       return (String)risultato.get("name");
    }
     public int getfedsdnSiteID(String name, String tenant){
       DB database = this.getDB(tenant);
       DBCollection collection = database.getCollection("fedsdnSite");
       BasicDBObject researchField = new BasicDBObject("siteEntry.name", name);
       DBObject risultato = collection.findOne(researchField);
       Object r=((Number) risultato.get("siteID"));
       if(r instanceof Integer)
           return ((Integer) r).intValue();
       else
        return ((Double)r).intValue();//((Number) mapObj.get("autostart")).intValue()//(float) ((double) result.get(v))
    }
     
    /*
    public void insertfedsdnFednet(String json){
        
        DB dataBase = this.getDB(this.identityDB);
        DBCollection collezione = dataBase.getCollection("fedsdnFednet");
        BasicDBObject obj = (BasicDBObject) JSON.parse(json);
        obj.append("insertTimestamp", System.currentTimeMillis());
        collezione.save(obj);
    }
    */
    public void insertfedsdnFednet(String json, String tenant){
        
        DB dataBase = this.getDB(tenant);
        DBCollection collezione = dataBase.getCollection("fedsdnFednet");
        BasicDBObject obj = (BasicDBObject) JSON.parse(json);
        obj.append("insertTimestamp", System.currentTimeMillis());
        collezione.save(obj);
    }
    
    
    
    /**
     * Update NetSegments on Mongo
     * @param dbName
     * @param fednetName
     * @param jsonWnetseg
     * @return 
     * 25/09/2017DA TESTARE PRIMA DI USARE
     */
    public boolean updatefsdnfednet_netseg(String dbName, String fednetName,String jsonWnetseg ) {
        boolean result = true;
        BasicDBObject first = new BasicDBObject();
        first.put("fednetEntry.name", fednetName);
        BasicDBObject sortquery = new BasicDBObject();
        sortquery.put("insertTimestamp", -1);
       
        DB database = this.getDB(dbName);
        DBCollection collection = database.getCollection("fedsdnFednet");
        DBObject obj=this.conditionedResearch_Obj(collection, first, sortquery);
        if(obj==null)
            result=false;
        else
        {
            obj = (BasicDBObject) collection.findOne(first);
            obj.removeField("fednetEntry.netsegmets");
            obj.put("fednetEntry.netsegmets", jsonWnetseg);
            collection.save(obj);
        }
        return result;
    }
    /*
    public String getfedsdnFednet(String federationTenantName){
       DB database = this.getDB(this.identityDB);
       DBCollection collection = database.getCollection("fedsdnFednet");
       BasicDBObject researchField = new BasicDBObject("federationTenantName", federationTenantName);
       DBObject risultato = collection.findOne(researchField);
       return risultato.toString();
    }*/
    
    public ArrayList getfedsdnFednetIDs(String federationTenantName){
       DB database = this.getDB(this.identityDB);
       DBCollection collection = database.getCollection("fedsdnFednet");
       BasicDBObject researchField = new BasicDBObject("federationTenantName", federationTenantName);
       DBCursor risultato = collection.find(researchField);
       Iterator it=risultato.iterator();
       ArrayList<Integer> resList=new ArrayList();
       while(it.hasNext()){
       
       resList.add(((Number) ((DBObject)it.next()).get("id")).intValue());
       }
       return resList;//((Number) mapObj.get("autostart")).intValue()//(float) ((double) result.get(v))
    }
     
    public String getfedsdnFednet(String fednet_name, String tenant){
       DB database = this.getDB(tenant);
       DBCollection collection = database.getCollection("fedsdnFednet");
       BasicDBObject researchField = new BasicDBObject("fednetEntry.name", fednet_name);
       DBObject risultato = collection.findOne(researchField);
       return risultato.toString();
    }
    public String getfedsdnnameFednet(long fednetId, String tenant)throws JSONException{
       DB database = this.getDB(tenant);
       DBCollection collection = database.getCollection("fedsdnFednet");
       BasicDBObject researchField = new BasicDBObject("fednetID", fednetId);
       DBObject risultato = collection.findOne(researchField);
       JSONObject finalres=new JSONObject((String)risultato.get("fednetEntry"));
       return finalres.getString("name");
    }
    public String getfedsdnFednet(long fednetId, String tenant)throws JSONException{
       DB database = this.getDB(tenant);
       DBCollection collection = database.getCollection("fedsdnFednet");
       BasicDBObject researchField = new BasicDBObject("fednetID", fednetId);
       DBObject risultato = collection.findOne(researchField);
       JSONObject finalres=new JSONObject((String)risultato.get("fednetEntry"));
       return finalres.toString(0);
    }
     public int getfedsdnFednetID(String fednet_name, String tenant){
       DB database = this.getDB(tenant);
       DBCollection collection = database.getCollection("fedsdnFednet");
       BasicDBObject researchField = new BasicDBObject("fednetEntry.name", fednet_name);
       DBObject risultato = collection.findOne(researchField);
       Object ob= risultato.get("fednetID");
       if(ob instanceof String)
           return new Integer((String)ob).intValue();
       else
           return ((Number) risultato.get("fednetID")).intValue();//((Number) mapObj.get("autostart")).intValue()//(float) ((double) result.get(v))
    }
     
    //verificare se la parte dei netsegment và qui o và memorizzata nel DB del tenant
    /*public void insertfedsdnNetSeg(String json){
        
        DB dataBase = this.getDB(this.identityDB);
        DBCollection collezione = dataBase.getCollection("fedsdnNetSeg");
        BasicDBObject obj = (BasicDBObject) JSON.parse(json);
        obj.append("insertTimestamp", System.currentTimeMillis());
        collezione.save(obj);
    }
    
    public String getfedsdnNetSeg(String vnetName,String CloudID){
       DB database = this.getDB(this.identityDB);
       DBCollection collection = database.getCollection("fedsdnNetSeg");
       BasicDBObject researchField = new BasicDBObject("CloudID", CloudID).append("vnetName", vnetName);
       DBObject risultato = collection.findOne(researchField);
       return risultato.toString();
    }
     public int getfedsdnNetSegID(String vnetName,String CloudID){
       DB database = this.getDB(this.identityDB);
       DBCollection collection = database.getCollection("fedsdnNetSeg");
       BasicDBObject researchField = new BasicDBObject("CloudID", CloudID).append("vnetName", vnetName);172.17.5.73
       DBObject risultato = collection.findOne(researchField);
       
       return ((Number) risultato.get("id")).intValue();//((Number) mapObj.get("autostart")).intValue()//(float) ((double) result.get(v))
    }
    */ 
    /**
     * 
     * @param json
     * @param tenant
     * @param siteID
     * @param fednetID
     * @param referenceSite 
     */
     public void insertfedsdnNetSeg(String json, String tenant,int siteID,int fednetID,String referenceSite,String netsegid){
        DB dataBase = this.getDB(tenant);
        DBCollection collezione = dataBase.getCollection("fedsdnNetSeg");
        BasicDBObject obj= new BasicDBObject();
        obj.append("referenceSite", referenceSite);
        obj.append("fednetID", fednetID);
        obj.append("siteID", siteID);
        BasicDBObject inner_obj = (BasicDBObject) JSON.parse(json);
        obj.append("netsegID", netsegid);
        obj.append("netsegEntry", inner_obj);
        obj.append("insertTimestamp", System.currentTimeMillis());
        collezione.save(obj);
    }
    
    public String getfedsdnNetSeg(String vnetName,String CloudID, String tenant){
       DB database = this.getDB(tenant);
       DBCollection collection = database.getCollection("fedsdnNetSeg");
       BasicDBObject researchField = new BasicDBObject("referenceSite", CloudID).append("netsegEntry.name", vnetName);
       DBObject risultato = collection.findOne(researchField);
       return risultato.toString();
    }
     public int getfedsdnNetSegID(String vnetName,String CloudID, String tenant){
       DB database = this.getDB(tenant);
       DBCollection collection = database.getCollection("fedsdnNetSeg");
       BasicDBObject researchField = new BasicDBObject("referenceSite", CloudID).append("netsegEntry.name", vnetName);
       DBObject risultato = collection.findOne(researchField);
       return ((Number) risultato.get("id")).intValue();//((Number) mapObj.get("autostart")).intValue()//(float) ((double) result.get(v))
    }
     
//</editor-fold>    
    //<editor-fold defaultstate="collapsed" desc="NetworkId Management">
    
    /**
     * This method is used to add inside "netIDMatch" collection, the document that represent netsegment infoes
     * make a correlation with internal OpenstackID.
     * @param dbName
     * @param fedsdnID
     * @param cloudId
     * @param internalNetworkId
     * @return 
     * @author gtricomi
     */
    public boolean storeInternalNetworkID(String dbName,String fedsdnID,String cloudId,String internalNetworkId ){
        
        DB dataBase = this.getDB(dbName);
        DBCollection collezione = this.getCollection(dataBase, "netIDMatch");
        BasicDBObject obj = new BasicDBObject();
        obj.put("fedsdnID", fedsdnID);
        obj.put("cloudId",cloudId);
        obj.put("internalNetworkId",internalNetworkId);
        obj.append("insertTimestamp", System.currentTimeMillis());
        collezione.save(obj);
        return true;
    }
    
    /**
     * This method is used to update inside "netIDMatch" collection, the document that represent netsegment infoes
     * make a correlation with internal OpenstackID.
     * @param dbName, tenant name
     * @param fedsdnID, netsegment id stored on FEDSDN
     * @param cloudId
     * @param internalNetworkId
     * @return 
     * @author gtricomi
     */
    public boolean updateInternalNetworkID(String dbName,String fedsdnID,String cloudId,String internalNetworkId ){
        
        DB dataBase = this.getDB(dbName);
        DBCollection collezione = this.getCollection(dataBase, "netIDMatch");
        BasicDBObject obj = new BasicDBObject();
        obj.put("fedsdnID", fedsdnID);
        obj.put("cloudId",cloudId);
        obj = (BasicDBObject) collezione.findOne(obj);
        if(obj==null){
            LOGGER.error("Document with fedsdnID:"+fedsdnID+" ,cloudId :"+cloudId+" for tenant :"+dbName+" isn't found!");
            return false;
        }
        obj.replace("internalNetworkId",internalNetworkId);
        obj.append("insertTimestamp", System.currentTimeMillis());
        collezione.save(obj);
        return true;
    }
    
    /**
     * 
     * @param fedsdnID
     * @param dbName, name of the Db where execute the operation
     * @return 
     * @author gtricomi
     */
    public String getInternalNetworkID(String dbName,String fedsdnID,String cloudId){
        DB dataBase = this.getDB(dbName);
        DBCollection collezione = dataBase.getCollection("netIDMatch");
        BasicDBObject obj = new BasicDBObject("fedsdnID", fedsdnID);
        obj.put("cloudId", cloudId);
        DBObject risultato = collezione.findOne(obj);
        if(risultato==null)
            return null;
        return risultato.get("internalNetworkId").toString();
    }
    //</editor-fold>
    
    
    //<editor-fold defaultstate="collapsed" desc="Cidr Information management">
    /**
     * 
     * @param tenantName
     * @param cidr
     * @param fednets
     * @param netsegments
     * @param cloudId 
     * @author gtricomi
     */
    public void insertcidrInfoes(
            String tenantName,
            String cidr,
            String fednets, 
            String netsegments,
            String cloudId
    ){
        DB dataBase = this.getDB(tenantName);
        DBCollection collezione = this.getCollection(dataBase, "cidrInfo");
        BasicDBObject obj = new BasicDBObject();
        obj.put("netsegments", netsegments);
        obj.put("cloudId",cloudId);
        obj.put("fednets",fednets);
        obj.put("cidr",cidr);
        obj.append("insertTimestamp", System.currentTimeMillis());
        collezione.save(obj);
        
    }
    
    /**
     * This function take from Collection "cidrInfo" the element that match with field contained inside hashmap researchParams.
     * @param tenantName, DbName where the collection is stored
     * @param researchParams, container for research params
     * @return JSONObject/null, JSONObject when the criteria match, null object in other cases.
     * @author gtricomi
     */
    public org.json.JSONObject getcidrInfoes(
            String tenantName,
            HashMap researchParams
    ) throws Exception{
        DB dataBase = this.getDB(tenantName);
        DBCollection collezione = dataBase.getCollection("cidrInfo");
        Iterator i=researchParams.keySet().iterator();
        BasicDBObject obj = new BasicDBObject();
        while(i.hasNext()){
            String tmpkey=(String)i.next();
            obj.put(tmpkey, researchParams.get(tmpkey));
        }
        DBObject risultato = collezione.findOne(obj);
        if(risultato==null)
            return null;
        try{
            return new org.json.JSONObject(risultato.toString());
        }
        catch(Exception e){
            LOGGER.error("Error occurred in DBObject parsing operation for getcidrInfoes.\n"+e.getMessage());
            throw e;
        }
     
    }
    
    //</editor-fold>
    
    
    
    /*
    This function it will be used in order to verify if the Network infos passed to construct the Fa table are corrected
    public String getNetTables(String tableName,String site_UUID_VM,String remote_UUID_VM,String tenant,DBMongo m){
        //HomeVM information retrieving
        BasicDBObject queryRunTime=new BasicDBObject("deviceId", site_UUID_VM);
        DBObject partial_RunTime_Result_H=m.find(tenant,"runTimeInfo" , queryRunTime);
        BasicDBObject queryPortInfo=new BasicDBObject("phisicalResourceId", site_UUID_VM);
        DBObject partial_PortInfo_Result_H=m.find(tenant,"runTimeInfo" , queryPortInfo);
        //RemoteVM information retrieving
        queryRunTime=new BasicDBObject("deviceId", remote_UUID_VM);
        DBObject partial_RunTime_Result_R=m.find(tenant,"runTimeInfo" , queryRunTime);
        queryPortInfo=new BasicDBObject("phisicalResourceId", remote_UUID_VM);
        DBObject partial_PortInfo_Result_R=m.find(tenant,"runTimeInfo" , queryPortInfo);
        
        
        BasicDBObject netTables_Object=new BasicDBObject(),netTable_Object=new BasicDBObject();
        
        if((tableName.equals(""))||(tableName==null))
            tableName=java.util.UUID.randomUUID().toString();
        
        //verificare presenza pregressa Table in tal caso recuperare versione
        ////serve nome collezione per questo elemento
        
        netTable_Object.put("Name",tableName);
        //inserire validazione per le stringe passate sotto????
        netTable_Object.put((String)partial_RunTime_Result_H.get("idCloud"),(String)partial_PortInfo_Result_H.get("networkId"));
        netTable_Object.put((String)partial_RunTime_Result_R.get("idCloud"),(String)partial_PortInfo_Result_R.get("networkId"));
        
        
    } */ 
    
  
    
    
    public String retrieveONEFlowTemplate(String tenant, String ManifestName, String onetempNAME) throws MDBIException {
        DBObject query = new com.mongodb.BasicDBObject();
        query.put("masterKey", ManifestName);
        JSONObject manifest = null;
        try {
            manifest = new JSONObject(this.getObj(tenant, "master", query.toString()));
        } catch (JSONException ex) {
            LOGGER.error("Impossible create a JSONObject form the Object retrieved by master collection");
        } catch (MDBIException ex) {
            LOGGER.error("Impossible create a JSONObject form the Object retrieved by master collection");
        }
        query = new com.mongodb.BasicDBObject();
        query.put("type", "ONE::Beacon::OneFlowTemplate");
        query.put("nome", onetempNAME);

        JSONArray ja = (JSONArray) manifest.remove("resources");
        String[] arquer = new String[ja.length()];
        for (int i = 0; i < ja.length(); i++) {
            try {
                arquer[i] = ja.getString(i);
            } catch (JSONException ex) {
                LOGGER.error("Error in Manifest resources identification, impossible analyze Manifest stored on Database");
                throw new MDBIException("Error in Manifest resources identification, impossible analyze Manifest stored on Database");
            }
        }

        try {
            query.put("uuid", new com.mongodb.BasicDBObject("$in", arquer));
            DBObject result = this.find(tenant, "resources", query);
            result = ((DBObject) result.get("properties"));
            result = ((DBObject) result.get("onetemplate"));
            return result.toString();

        } catch (Exception ex) {
            LOGGER.error("Impossible create a JSONObject with the oneFlow Template retrieved from Manifest");
            throw new MDBIException("Impossible create a JSONObject with the oneFlow Template retrieved from Manifest");
        }
       
    }
    
    

    
    
    
    public String retrieveFedNet(String tenant, String refSite) throws MDBIException {

        ArrayList<JSONObject> netNames = new ArrayList<JSONObject>();
        BasicDBObject allQuery = new BasicDBObject();
        BasicDBObject fields = new BasicDBObject();
        fields.put("fedNet", 1);
        
//        this.insert(tenant, "NetTablesInfo", jsonTable);
        try {
            DB database = this.getDB(tenant);
            DBCollection collection = database.getCollection("fednetsinSite");
            //BasicDBObject resQuery=new BasicDBObject("fedNet",refSite);
            BasicDBObject resQuery = new BasicDBObject("referenceSite", refSite);
            BasicDBObject sortQuery = new BasicDBObject("version", -1);
            //return conditionedResearch(collection, resQuery, sortQuery, fields);
            return conditionedResearch(collection, resQuery, sortQuery);

        } catch (Exception e) {

            throw new MDBIException();

}

        //DBCursor cursor =collection.find(allQuery,fields);
        //Iterator<DBObject> it = cursor.iterator();
        //ArrayList<String> net = new ArrayList();
        //while (it.hasNext()) {
        //     net.add(it.next().toString()); //array list di
        // }
        // return this.conditionedResearch(collection,resQuery,sortQuery,fields); //da modificare ritorna un solo valore per il singolo refsite
        //return result;
    }
    
    /**
     *
     * @param tenant
     * @param refSite
     * @return
     * @throws MDBIException
     * @author Alfonso Panarello
     */
    public ArrayList<String> retrieveBNANetSegFromFednet(String tenant, String refSite, Integer version, String fedNet) throws MDBIException {

        //ArrayList<JSONObject > netNames= new ArrayList<JSONObject>();
        BasicDBObject allQuery = new BasicDBObject();
        BasicDBObject fields = new BasicDBObject();
        BasicDBObject field = new BasicDBObject();

        ArrayList<String> netEntries = new ArrayList<>();
        fields.put("UUID", 1);
        field.put("netEntry", 1);
        String result = "";
//        this.insert(tenant, "NetTablesInfo", jsonTable);
        try {
            DB database = this.getDB(tenant);
            DBCollection collection = database.getCollection("BNATableData");
            //BasicDBObject resQuery=new BasicDBObject("fedNet",refSite);

            Object o = null;
            BasicDBObject resQuery = new BasicDBObject("referenceSite", refSite).append("version", version).append("fedNet", fedNet);
            DBCursor uuid = collection.find(resQuery);
            System.out.println("");
            if (!uuid.hasNext()) {
                return null;
            } else {
                o = uuid.next();

                collection = database.getCollection("BNANetSeg");
                BasicDBObject bdo = (BasicDBObject) o;

                resQuery = new BasicDBObject("FK", (String) bdo.get("FK"));
                System.out.println(resQuery);
                DBCursor fedNetArray = collection.find(resQuery, field);
                if (fedNetArray == null) {
                    System.out.println("fedNetArray - - -  NULL");;
                } else {
                    while (fedNetArray.hasNext()) {

                        BasicDBObject tempObj = (BasicDBObject) fedNetArray.next();
                        tempObj.removeField("_id");
                        
                        netEntries.add((tempObj.get("netEntry")).toString());

                    }
                }
                System.out.println("NET ENTRIES: " +netEntries.toString());
                return netEntries;
            }
        } catch (Exception e) {

            throw new MDBIException();

        }

    }
    
    
    
    
    
    
    
   /* 
    public String retrieveONEFlowTemplate(String tenant, String ManifestName, String onetempNAME) throws MDBIException {
        DBObject query = new com.mongodb.BasicDBObject();
        query.put("masterKey", ManifestName);
        JSONObject manifest = null;
        try {
            manifest = new JSONObject(this.getObj(tenant, "master", query.toString()));
        } catch (JSONException ex) {
            LOGGER.error("Impossible create a JSONObject form the Object retrieved by master collection");
        } catch (MDBIException ex) {
            LOGGER.error("Impossible create a JSONObject form the Object retrieved by master collection");
        }
        query = new com.mongodb.BasicDBObject();
        query.put("type", "ONE::Beacon::OneFlowTemplate");
        query.put("nome", onetempNAME);

        JSONArray ja = (JSONArray) manifest.remove("resources");
        String[] arquer = new String[ja.length()];
        for (int i = 0; i < ja.length(); i++) {
            try {
                arquer[i] = ja.getString(i);
            } catch (JSONException ex) {
                LOGGER.error("Error in Manifest resources identification, impossible analyze Manifest stored on Database");
                throw new MDBIException("Error in Manifest resources identification, impossible analyze Manifest stored on Database");
            }
        }

        try {
            query.put("uuid", new com.mongodb.BasicDBObject("$in", arquer));
            DBObject result = this.find(tenant, "resources", query);
            result = ((DBObject) result.get("properties"));
            result = ((DBObject) result.get("onetemplate"));
            return result.toString();

        } catch (Exception ex) {
            LOGGER.error("Impossible create a JSONObject with the oneFlow Template retrieved from Manifest");
            throw new MDBIException("Impossible create a JSONObject with the oneFlow Template retrieved from Manifest");
        }
       
    }
    */
    
}

