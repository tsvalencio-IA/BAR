package com.thiaguinho.controlebar;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;

public class DatabaseHelper extends SQLiteOpenHelper {
    private static final String DB_NAME = "controle_bar.db";
    private static final int DB_VERSION = 5;

    public DatabaseHelper(Context context) { super(context, DB_NAME, null, DB_VERSION); }

    @Override public void onConfigure(SQLiteDatabase db) {
        super.onConfigure(db);
        db.setForeignKeyConstraintsEnabled(true);
    }

    @Override public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE products (id INTEGER PRIMARY KEY AUTOINCREMENT,name TEXT NOT NULL,category TEXT DEFAULT '',barcode TEXT DEFAULT '',cost REAL DEFAULT 0,price REAL DEFAULT 0,stock REAL DEFAULT 0,min_stock REAL DEFAULT 0,unit TEXT DEFAULT 'un',supplier TEXT DEFAULT '',active INTEGER DEFAULT 1,created_at TEXT NOT NULL,updated_at TEXT NOT NULL)");
        db.execSQL("CREATE TABLE sales (id INTEGER PRIMARY KEY AUTOINCREMENT,total REAL DEFAULT 0,payment_method TEXT DEFAULT 'Dinheiro',notes TEXT DEFAULT '',sold_at TEXT NOT NULL,table_name TEXT DEFAULT '',status TEXT DEFAULT 'FECHADA',closed_at TEXT DEFAULT '',origin_device TEXT DEFAULT '',remote_id TEXT DEFAULT '',imported_at TEXT DEFAULT '')");
        db.execSQL("CREATE TABLE sale_items (id INTEGER PRIMARY KEY AUTOINCREMENT,sale_id INTEGER NOT NULL,product_id INTEGER,product_name TEXT NOT NULL,quantity REAL DEFAULT 0,unit_price REAL DEFAULT 0,subtotal REAL DEFAULT 0,origin_device TEXT DEFAULT '',remote_id TEXT DEFAULT '',FOREIGN KEY(sale_id) REFERENCES sales(id) ON DELETE CASCADE)");
        db.execSQL("CREATE TABLE stock_movements (id INTEGER PRIMARY KEY AUTOINCREMENT,product_id INTEGER NOT NULL,type TEXT NOT NULL,quantity REAL NOT NULL,reason TEXT DEFAULT '',created_at TEXT NOT NULL,origin_device TEXT DEFAULT '',remote_id TEXT DEFAULT '',FOREIGN KEY(product_id) REFERENCES products(id))");
        db.execSQL("CREATE INDEX idx_products_name ON products(name)");
        db.execSQL("CREATE INDEX idx_products_supplier ON products(supplier)");
        db.execSQL("CREATE INDEX idx_sales_date ON sales(sold_at)");
        db.execSQL("CREATE INDEX idx_sales_remote ON sales(remote_id)");
        db.execSQL("CREATE INDEX idx_sale_items_sale ON sale_items(sale_id)");
        db.execSQL("CREATE INDEX idx_stock_movements_product ON stock_movements(product_id)");
    }

    @Override public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            addColumn(db, "products", "active", "INTEGER DEFAULT 1");
            db.execSQL("CREATE TABLE IF NOT EXISTS stock_movements (id INTEGER PRIMARY KEY AUTOINCREMENT,product_id INTEGER NOT NULL,type TEXT NOT NULL,quantity REAL NOT NULL,reason TEXT DEFAULT '',created_at TEXT NOT NULL,FOREIGN KEY(product_id) REFERENCES products(id))");
            db.execSQL("CREATE INDEX IF NOT EXISTS idx_stock_movements_product ON stock_movements(product_id)");
        }
        if (oldVersion < 3) {
            addColumn(db, "sales", "table_name", "TEXT DEFAULT ''");
            addColumn(db, "sales", "status", "TEXT DEFAULT 'FECHADA'");
            addColumn(db, "sales", "closed_at", "TEXT DEFAULT ''");
            try { db.execSQL("UPDATE sales SET status='FECHADA', closed_at=sold_at WHERE status IS NULL OR status=''"); } catch (Exception ignored) {}
        }
        if (oldVersion < 4) {
            addColumn(db, "sales", "origin_device", "TEXT DEFAULT ''");
            addColumn(db, "sales", "remote_id", "TEXT DEFAULT ''");
            addColumn(db, "sales", "imported_at", "TEXT DEFAULT ''");
            addColumn(db, "sale_items", "origin_device", "TEXT DEFAULT ''");
            addColumn(db, "sale_items", "remote_id", "TEXT DEFAULT ''");
            addColumn(db, "stock_movements", "origin_device", "TEXT DEFAULT ''");
            addColumn(db, "stock_movements", "remote_id", "TEXT DEFAULT ''");
            db.execSQL("CREATE INDEX IF NOT EXISTS idx_sales_remote ON sales(remote_id)");
        }
        if (oldVersion < 5) {
            addColumn(db, "products", "supplier", "TEXT DEFAULT ''");
            db.execSQL("CREATE INDEX IF NOT EXISTS idx_products_supplier ON products(supplier)");
        }
    }

    private void addColumn(SQLiteDatabase db, String table, String column, String def) {
        try { db.execSQL("ALTER TABLE " + table + " ADD COLUMN " + column + " " + def); } catch (Exception ignored) {}
    }

    public long saveProduct(Long id, String name, String category, String barcode, String supplier, double cost, double price, double stock, double minStock, String unit, String now) {
        SQLiteDatabase database = getWritableDatabase();
        database.beginTransaction();
        try {
            ContentValues v = new ContentValues();
            v.put("name", name); v.put("category", category); v.put("barcode", barcode); v.put("supplier", supplier==null?"":supplier);
            v.put("cost", cost); v.put("price", price); v.put("stock", stock);
            v.put("min_stock", minStock); v.put("unit", unit); v.put("active", 1); v.put("updated_at", now);
            long result;
            if (id == null) { v.put("created_at", now); result = database.insertOrThrow("products", null, v); }
            else { database.update("products", v, "id=?", new String[]{String.valueOf(id)}); result = id; }
            database.setTransactionSuccessful();
            return result;
        } finally { database.endTransaction(); }
    }

    public Cursor product(long id) { return getReadableDatabase().rawQuery("SELECT * FROM products WHERE id=?", new String[]{String.valueOf(id)}); }
    public int productCount() { Cursor c=getReadableDatabase().rawQuery("SELECT COUNT(*) FROM products",null); try{return c.moveToFirst()?c.getInt(0):0;}finally{c.close();} }
    public void archiveProduct(long id) { ContentValues v=new ContentValues(); v.put("active",0); getWritableDatabase().update("products",v,"id=?",new String[]{String.valueOf(id)}); }

    public void adjustStock(long id, double delta, String type, String reason, String now) throws Exception {
        if (delta == 0) throw new Exception("Informe uma quantidade diferente de zero.");
        SQLiteDatabase database=getWritableDatabase(); database.beginTransaction();
        try {
            Cursor c=database.rawQuery("SELECT stock FROM products WHERE id=? AND active=1",new String[]{String.valueOf(id)});
            if(!c.moveToFirst()){c.close();throw new Exception("Produto não encontrado.");}
            double current=c.getDouble(0); c.close();
            double next=current+delta;
            if(next<0) throw new Exception("O estoque não pode ficar negativo.");
            database.execSQL("UPDATE products SET stock=?,updated_at=? WHERE id=?",new Object[]{next,now,id});
            ContentValues m=new ContentValues(); m.put("product_id",id);m.put("type",type);m.put("quantity",delta);m.put("reason",reason);m.put("created_at",now);m.put("origin_device","");m.put("remote_id","");
            database.insertOrThrow("stock_movements",null,m);
            database.setTransactionSuccessful();
        } finally { database.endTransaction(); }
    }

    public Cursor products(String search) {
        String q=search==null?"":search.trim();
        if(q.isEmpty()) return getReadableDatabase().rawQuery("SELECT * FROM products WHERE active=1 ORDER BY name COLLATE NOCASE",null);
        return getReadableDatabase().rawQuery("SELECT * FROM products WHERE active=1 AND (name LIKE ? OR category LIKE ? OR barcode LIKE ? OR supplier LIKE ?) ORDER BY name COLLATE NOCASE",new String[]{"%"+q+"%","%"+q+"%","%"+q+"%","%"+q+"%"});
    }

    public Cursor lowStockProducts() { return getReadableDatabase().rawQuery("SELECT *,CASE WHEN min_stock-stock>0 THEN min_stock-stock ELSE 0 END AS suggested FROM products WHERE active=1 AND stock<=min_stock ORDER BY supplier COLLATE NOCASE,(min_stock-stock) DESC,name COLLATE NOCASE",null); }
    public Cursor lowStockProductsBySupplier(String supplier) { String s=supplier==null?"":supplier; return getReadableDatabase().rawQuery("SELECT *,CASE WHEN min_stock-stock>0 THEN min_stock-stock ELSE 0 END AS suggested FROM products WHERE active=1 AND stock<=min_stock AND IFNULL(supplier,'')=? ORDER BY name COLLATE NOCASE",new String[]{s}); }
    public Cursor suppliersNeedingPurchase() { return getReadableDatabase().rawQuery("SELECT IFNULL(supplier,'') AS supplier,COUNT(*) AS total,SUM(CASE WHEN min_stock-stock>0 THEN min_stock-stock ELSE 0 END) AS suggested_total FROM products WHERE active=1 AND stock<=min_stock GROUP BY IFNULL(supplier,'') ORDER BY CASE WHEN IFNULL(supplier,'')='' THEN 1 ELSE 0 END,supplier COLLATE NOCASE",null); }
    public Cursor stockMovements(long productId) { return getReadableDatabase().rawQuery("SELECT * FROM stock_movements WHERE product_id=? ORDER BY created_at DESC LIMIT 100",new String[]{String.valueOf(productId)}); }

    public long createSale(JSONArray items,String paymentMethod,String notes,String soldAt) throws Exception {
        return createSaleInternal(items,paymentMethod,notes,soldAt,"","FECHADA",soldAt,null);
    }

    public long addItemsToOpenTable(JSONArray items,String tableName,String notes,String now) throws Exception {
        if(tableName==null||tableName.trim().isEmpty()) throw new Exception("Informe a mesa.");
        long saleId=getOpenSaleId(tableName.trim());
        return createSaleInternal(items,"Mesa aberta",notes,now,tableName.trim(),"ABERTA","",saleId>0?saleId:null);
    }

    public long closeOpenTable(String tableName,String paymentMethod,String notes,String closedAt) throws Exception {
        if(tableName==null||tableName.trim().isEmpty()) throw new Exception("Informe a mesa.");
        SQLiteDatabase database=getWritableDatabase(); database.beginTransaction();
        try{
            long id=getOpenSaleId(database,tableName.trim());
            if(id<=0) throw new Exception("Não existe atendimento aberto para "+tableName+".");
            ContentValues v=new ContentValues();
            v.put("payment_method",paymentMethod==null||paymentMethod.trim().isEmpty()?"Dinheiro":paymentMethod.trim());
            v.put("status","FECHADA");
            v.put("closed_at",closedAt);
            if(notes!=null&&!notes.trim().isEmpty()) v.put("notes",notes.trim());
            database.update("sales",v,"id=?",new String[]{String.valueOf(id)});
            database.setTransactionSuccessful();
            return id;
        } finally { database.endTransaction(); }
    }

    public long getOpenSaleId(String tableName){return getOpenSaleId(getReadableDatabase(),tableName);}
    private long getOpenSaleId(SQLiteDatabase database,String tableName){Cursor c=database.rawQuery("SELECT id FROM sales WHERE table_name=? AND IFNULL(status,'FECHADA')='ABERTA' ORDER BY id DESC LIMIT 1",new String[]{tableName});try{return c.moveToFirst()?c.getLong(0):0;}finally{c.close();}}

    private long createSaleInternal(JSONArray items,String paymentMethod,String notes,String soldAt,String tableName,String status,String closedAt,Long existingSaleId) throws Exception {
        if(items==null||items.length()==0) throw new Exception("Adicione ao menos um item.");
        SQLiteDatabase database=getWritableDatabase(); database.beginTransaction();
        try {
            double total=0;
            java.util.HashMap<Long,Double> qtyByProduct=new java.util.HashMap<>();
            java.util.HashMap<Long,String> nameByProduct=new java.util.HashMap<>();
            for(int i=0;i<items.length();i++){
                JSONObject item=items.getJSONObject(i); long productId=item.optLong("productId",0); double qty=item.getDouble("quantity"); double subtotal=item.getDouble("subtotal");
                if(qty<=0||subtotal<0) throw new Exception("Item inválido no carrinho.");
                if(productId>0){qtyByProduct.put(productId,qtyByProduct.containsKey(productId)?qtyByProduct.get(productId)+qty:qty);nameByProduct.put(productId,item.getString("productName"));}
                total+=subtotal;
            }
            for(Long productId:qtyByProduct.keySet()){
                Cursor c=database.rawQuery("SELECT stock,active FROM products WHERE id=?",new String[]{String.valueOf(productId)});
                if(!c.moveToFirst()){c.close();throw new Exception("Um produto do carrinho não existe mais.");}
                double stock=c.getDouble(0);int active=c.getInt(1);c.close();
                if(active!=1)throw new Exception("Um produto do carrinho está inativo.");
                double requested=qtyByProduct.get(productId);
                if(requested>stock+0.000001)throw new Exception("Estoque insuficiente para "+nameByProduct.get(productId)+". Disponível: "+formatNumber(stock)+". Solicitado: "+formatNumber(requested));
            }
            long saleId;
            if(existingSaleId!=null&&existingSaleId>0){
                saleId=existingSaleId;
                database.execSQL("UPDATE sales SET total=total+?,notes=CASE WHEN ?='' THEN notes ELSE CASE WHEN notes='' THEN ? ELSE notes||' | '||? END END WHERE id=?",new Object[]{round2(total),notes==null?"":notes.trim(),notes==null?"":notes.trim(),notes==null?"":notes.trim(),saleId});
            }else{
                ContentValues sale=new ContentValues();sale.put("total",round2(total));sale.put("payment_method",paymentMethod);sale.put("notes",notes);sale.put("sold_at",soldAt);sale.put("table_name",tableName);sale.put("status",status);sale.put("closed_at",closedAt);sale.put("origin_device","");sale.put("remote_id","");sale.put("imported_at","");
                saleId=database.insertOrThrow("sales",null,sale);
            }
            for(int i=0;i<items.length();i++){
                JSONObject item=items.getJSONObject(i);long productId=item.optLong("productId",0);double qty=item.getDouble("quantity");
                ContentValues si=new ContentValues();si.put("sale_id",saleId);if(productId>0)si.put("product_id",productId);si.put("product_name",item.getString("productName"));si.put("quantity",qty);si.put("unit_price",item.getDouble("unitPrice"));si.put("subtotal",round2(item.getDouble("subtotal")));si.put("origin_device","");si.put("remote_id","");database.insertOrThrow("sale_items",null,si);
                if(productId>0){database.execSQL("UPDATE products SET stock=stock-?,updated_at=? WHERE id=?",new Object[]{qty,soldAt,productId});ContentValues m=new ContentValues();m.put("product_id",productId);m.put("type",IFNULL(status).equals("ABERTA")?"MESA":"VENDA");m.put("quantity",-qty);m.put("reason",(IFNULL(status).equals("ABERTA")?"Mesa "+tableName:"Venda")+" #"+saleId);m.put("created_at",soldAt);m.put("origin_device","");m.put("remote_id","");database.insertOrThrow("stock_movements",null,m);}
            }
            database.setTransactionSuccessful(); return saleId;
        } finally { database.endTransaction(); }
    }
    private String IFNULL(String s){return s==null?"":s;}

    public Cursor salesForDay(String date){return getReadableDatabase().rawQuery("SELECT s.*,COUNT(si.id) item_count FROM sales s LEFT JOIN sale_items si ON si.sale_id=s.id WHERE IFNULL(s.status,'FECHADA')<>'ABERTA' AND substr(CASE WHEN IFNULL(s.closed_at,'')='' THEN s.sold_at ELSE s.closed_at END,1,10)=? GROUP BY s.id ORDER BY CASE WHEN IFNULL(s.closed_at,'')='' THEN s.sold_at ELSE s.closed_at END DESC",new String[]{date});}
    public Cursor allSales(){return getReadableDatabase().rawQuery("SELECT s.*,COUNT(si.id) item_count FROM sales s LEFT JOIN sale_items si ON si.sale_id=s.id WHERE IFNULL(s.status,'FECHADA')<>'ABERTA' GROUP BY s.id ORDER BY CASE WHEN IFNULL(s.closed_at,'')='' THEN s.sold_at ELSE s.closed_at END DESC",null);}
    public Cursor openTables(){return getReadableDatabase().rawQuery("SELECT s.*,COUNT(si.id) item_count FROM sales s LEFT JOIN sale_items si ON si.sale_id=s.id WHERE IFNULL(s.status,'FECHADA')='ABERTA' GROUP BY s.id ORDER BY s.table_name COLLATE NOCASE",null);}
    public Cursor openSaleForTable(String tableName){return getReadableDatabase().rawQuery("SELECT s.*,COUNT(si.id) item_count FROM sales s LEFT JOIN sale_items si ON si.sale_id=s.id WHERE s.table_name=? AND IFNULL(s.status,'FECHADA')='ABERTA' GROUP BY s.id LIMIT 1",new String[]{tableName});}
    public Cursor saleItems(long saleId){return getReadableDatabase().rawQuery("SELECT * FROM sale_items WHERE sale_id=? ORDER BY id",new String[]{String.valueOf(saleId)});}
    public double dailyTotal(String date){Cursor c=getReadableDatabase().rawQuery("SELECT COALESCE(SUM(total),0) FROM sales WHERE IFNULL(status,'FECHADA')<>'ABERTA' AND substr(CASE WHEN IFNULL(closed_at,'')='' THEN sold_at ELSE closed_at END,1,10)=?",new String[]{date});try{return c.moveToFirst()?c.getDouble(0):0;}finally{c.close();}}
    public int dailyCount(String date){Cursor c=getReadableDatabase().rawQuery("SELECT COUNT(*) FROM sales WHERE IFNULL(status,'FECHADA')<>'ABERTA' AND substr(CASE WHEN IFNULL(closed_at,'')='' THEN sold_at ELSE closed_at END,1,10)=?",new String[]{date});try{return c.moveToFirst()?c.getInt(0):0;}finally{c.close();}}
    public Cursor paymentSummary(String date){return getReadableDatabase().rawQuery("SELECT payment_method,COUNT(*) sale_count,COALESCE(SUM(total),0) total FROM sales WHERE IFNULL(status,'FECHADA')<>'ABERTA' AND substr(CASE WHEN IFNULL(closed_at,'')='' THEN sold_at ELSE closed_at END,1,10)=? GROUP BY payment_method ORDER BY total DESC",new String[]{date});}
    public Cursor topProducts(String date){return getReadableDatabase().rawQuery("SELECT si.product_name,SUM(si.quantity) quantity,SUM(si.subtotal) total FROM sale_items si JOIN sales s ON s.id=si.sale_id WHERE IFNULL(s.status,'FECHADA')<>'ABERTA' AND substr(CASE WHEN IFNULL(s.closed_at,'')='' THEN s.sold_at ELSE s.closed_at END,1,10)=? GROUP BY si.product_name ORDER BY quantity DESC LIMIT 20",new String[]{date});}

    public JSONObject exportAll() throws Exception {JSONObject root=new JSONObject();root.put("app","Controle de Vendas e Estoque do Bar");root.put("format","thiaguinho-bar-backup-v3");root.put("products",cursorToJson(getReadableDatabase().rawQuery("SELECT * FROM products ORDER BY id",null)));root.put("sales",cursorToJson(getReadableDatabase().rawQuery("SELECT * FROM sales ORDER BY id",null)));root.put("saleItems",cursorToJson(getReadableDatabase().rawQuery("SELECT * FROM sale_items ORDER BY id",null)));root.put("stockMovements",cursorToJson(getReadableDatabase().rawQuery("SELECT * FROM stock_movements ORDER BY id",null)));return root;}

    public JSONObject exportMovement(String date, String deviceId, String deviceRole, String now) throws Exception {
        if(deviceId==null||deviceId.trim().isEmpty()) deviceId="DISPOSITIVO";
        SQLiteDatabase database=getWritableDatabase();database.beginTransaction();
        try{ensureRemoteIdsForDate(database,date,deviceId.trim(),now);database.setTransactionSuccessful();}finally{database.endTransaction();}
        JSONObject root=new JSONObject();
        root.put("app","Controle do Bar");root.put("format","thiaguinho-bar-movimento-v1");root.put("movementDate",date);root.put("originDevice",deviceId.trim());root.put("originRole",deviceRole==null?"":deviceRole);root.put("exportedAt",now);
        root.put("products",cursorToJson(getReadableDatabase().rawQuery("SELECT id,name,category,barcode,supplier,unit,price FROM products ORDER BY id",null)));
        root.put("sales",cursorToJson(getReadableDatabase().rawQuery("SELECT * FROM sales WHERE IFNULL(status,'FECHADA')<>'ABERTA' AND substr(CASE WHEN IFNULL(closed_at,'')='' THEN sold_at ELSE closed_at END,1,10)=? ORDER BY id",new String[]{date})));
        root.put("saleItems",cursorToJson(getReadableDatabase().rawQuery("SELECT si.* FROM sale_items si JOIN sales s ON s.id=si.sale_id WHERE IFNULL(s.status,'FECHADA')<>'ABERTA' AND substr(CASE WHEN IFNULL(s.closed_at,'')='' THEN s.sold_at ELSE s.closed_at END,1,10)=? ORDER BY si.id",new String[]{date})));
        return root;
    }

    private void ensureRemoteIdsForDate(SQLiteDatabase database,String date,String deviceId,String now){
        Cursor c=database.rawQuery("SELECT id FROM sales WHERE IFNULL(status,'FECHADA')<>'ABERTA' AND substr(CASE WHEN IFNULL(closed_at,'')='' THEN sold_at ELSE closed_at END,1,10)=? AND (remote_id IS NULL OR remote_id='')",new String[]{date});
        try{while(c.moveToNext()){long id=c.getLong(0);String remote=deviceId+"-SALE-"+id;ContentValues v=new ContentValues();v.put("origin_device",deviceId);v.put("remote_id",remote);database.update("sales",v,"id=?",new String[]{String.valueOf(id)});}}
        finally{c.close();}
    }

    public JSONObject mergeMovement(JSONObject root, String now) throws Exception {
        JSONArray sales=root.optJSONArray("sales"),saleItems=root.optJSONArray("saleItems"),products=root.optJSONArray("products");
        if(sales==null||saleItems==null)throw new Exception("Arquivo de movimento incompatível."); if(products==null)products=new JSONArray();
        String origin=root.optString("originDevice","").trim(); if(origin.isEmpty()) origin="ATENDENTE";
        HashMap<Long,JSONObject> productMap=new HashMap<>();
        for(int i=0;i<products.length();i++){JSONObject p=products.getJSONObject(i);productMap.put(p.optLong("id",0),p);}
        int imported=0,skipped=0,itemCount=0,stockApplied=0,missingProducts=0;
        SQLiteDatabase database=getWritableDatabase();database.beginTransaction();
        try{
            for(int i=0;i<sales.length();i++){
                JSONObject s=sales.getJSONObject(i);String status=s.optString("status","FECHADA");if("ABERTA".equals(status))continue;
                long sourceSaleId=s.optLong("id",0);String remote=s.optString("remote_id","").trim();if(remote.isEmpty())remote=origin+"-SALE-"+sourceSaleId;
                if(remoteExists(database,remote)){skipped++;continue;}
                ContentValues sale=new ContentValues();sale.put("total",s.optDouble("total",0));sale.put("payment_method",s.optString("payment_method","Dinheiro"));String notes=s.optString("notes","");String importNote="Importado de "+origin+" em "+now; sale.put("notes",notes.trim().isEmpty()?importNote:notes+" | "+importNote);sale.put("sold_at",s.optString("sold_at",now));sale.put("table_name",s.optString("table_name",""));sale.put("status","FECHADA");sale.put("closed_at",s.optString("closed_at",s.optString("sold_at",now)));sale.put("origin_device",origin);sale.put("remote_id",remote);sale.put("imported_at",now);
                long localSaleId=database.insertOrThrow("sales",null,sale);imported++;
                for(int j=0;j<saleItems.length();j++){
                    JSONObject it=saleItems.getJSONObject(j);if(it.optLong("sale_id",0)!=sourceSaleId)continue;
                    long sourceProductId=it.optLong("product_id",0);String productName=it.optString("product_name","Produto importado");double qty=it.optDouble("quantity",0);double unitPrice=it.optDouble("unit_price",0);double subtotal=it.optDouble("subtotal",round2(qty*unitPrice));
                    JSONObject sourceProduct=productMap.get(sourceProductId);String barcode=sourceProduct==null?"":sourceProduct.optString("barcode","");long localProductId=findProductForImport(database,productName,barcode);
                    ContentValues si=new ContentValues();si.put("sale_id",localSaleId);if(localProductId>0)si.put("product_id",localProductId);si.put("product_name",productName);si.put("quantity",qty);si.put("unit_price",unitPrice);si.put("subtotal",round2(subtotal));si.put("origin_device",origin);si.put("remote_id",remote+"-ITEM-"+it.optLong("id",j));database.insertOrThrow("sale_items",null,si);itemCount++;
                    if(localProductId>0){database.execSQL("UPDATE products SET stock=stock-?,updated_at=? WHERE id=?",new Object[]{qty,now,localProductId});ContentValues m=new ContentValues();m.put("product_id",localProductId);m.put("type","IMPORTADO_ATENDENTE");m.put("quantity",-qty);m.put("reason","Importado de "+origin+" • venda "+remote);m.put("created_at",s.optString("closed_at",s.optString("sold_at",now)));m.put("origin_device",origin);m.put("remote_id",remote+"-MOV-"+it.optLong("id",j));database.insertOrThrow("stock_movements",null,m);stockApplied++;}else{missingProducts++;}
                }
            }
            database.setTransactionSuccessful();
        }finally{database.endTransaction();}
        JSONObject res=new JSONObject();res.put("importedSales",imported);res.put("skippedSales",skipped);res.put("items",itemCount);res.put("stockApplied",stockApplied);res.put("missingProducts",missingProducts);res.put("originDevice",origin);return res;
    }

    private boolean remoteExists(SQLiteDatabase database,String remote){Cursor c=database.rawQuery("SELECT id FROM sales WHERE remote_id=? LIMIT 1",new String[]{remote});try{return c.moveToFirst();}finally{c.close();}}
    private long findProductForImport(SQLiteDatabase database,String name,String barcode){
        if(barcode!=null&&!barcode.trim().isEmpty()){Cursor c=database.rawQuery("SELECT id FROM products WHERE active=1 AND barcode=? LIMIT 1",new String[]{barcode.trim()});try{if(c.moveToFirst())return c.getLong(0);}finally{c.close();}}
        Cursor c=database.rawQuery("SELECT id FROM products WHERE active=1 AND lower(trim(name))=lower(trim(?)) LIMIT 1",new String[]{name==null?"":name.trim()});try{return c.moveToFirst()?c.getLong(0):0;}finally{c.close();}
    }

    public void importAll(JSONObject root) throws Exception {
        JSONArray products=root.optJSONArray("products"),sales=root.optJSONArray("sales"),saleItems=root.optJSONArray("saleItems"),movements=root.optJSONArray("stockMovements");
        if(products==null||sales==null||saleItems==null)throw new Exception("Backup incompatível."); if(movements==null)movements=new JSONArray();
        SQLiteDatabase database=getWritableDatabase();database.beginTransaction();
        try{database.delete("stock_movements",null,null);database.delete("sale_items",null,null);database.delete("sales",null,null);database.delete("products",null,null);insertJsonRows(database,"products",products);insertJsonRows(database,"sales",sales);insertJsonRows(database,"sale_items",saleItems);insertJsonRows(database,"stock_movements",movements);database.setTransactionSuccessful();}finally{database.endTransaction();}
    }

    private JSONArray cursorToJson(Cursor c) throws Exception {JSONArray arr=new JSONArray();try{String[] cols=c.getColumnNames();while(c.moveToNext()){JSONObject o=new JSONObject();for(int i=0;i<cols.length;i++){switch(c.getType(i)){case Cursor.FIELD_TYPE_INTEGER:o.put(cols[i],c.getLong(i));break;case Cursor.FIELD_TYPE_FLOAT:o.put(cols[i],c.getDouble(i));break;case Cursor.FIELD_TYPE_STRING:o.put(cols[i],c.getString(i));break;case Cursor.FIELD_TYPE_NULL:o.put(cols[i],JSONObject.NULL);break;default:o.put(cols[i],c.getString(i));}}arr.put(o);}}finally{c.close();}return arr;}
    private void insertJsonRows(SQLiteDatabase db,String table,JSONArray rows)throws Exception{HashSet<String> allowed=tableColumns(db,table);for(int i=0;i<rows.length();i++){JSONObject o=rows.getJSONObject(i);ContentValues v=new ContentValues();JSONArray names=o.names();if(names==null)continue;for(int n=0;n<names.length();n++){String key=names.getString(n);if(!allowed.contains(key))continue;Object value=o.opt(key);if(value==null||value==JSONObject.NULL)v.putNull(key);else if(value instanceof Integer||value instanceof Long)v.put(key,((Number)value).longValue());else if(value instanceof Number)v.put(key,((Number)value).doubleValue());else v.put(key,String.valueOf(value));}db.insertOrThrow(table,null,v);}}
    private HashSet<String> tableColumns(SQLiteDatabase db,String table){HashSet<String> set=new HashSet<>();Cursor c=db.rawQuery("PRAGMA table_info("+table+")",null);try{while(c.moveToNext())set.add(c.getString(c.getColumnIndexOrThrow("name")));}finally{c.close();}return set;}
    private static double round2(double v){return Math.round(v*100.0)/100.0;}
    public static String money(double value){return String.format(new Locale("pt","BR"),"R$ %.2f",value).replace('.',',');}
    public static String formatNumber(double value){return String.format(new Locale("pt","BR"),"%.2f",value);}
}
