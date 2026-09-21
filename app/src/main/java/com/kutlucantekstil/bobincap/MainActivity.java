package com.kutlucantekstil.bobincap;

import android.app.*;
import android.os.Bundle;
import android.content.*;
import android.net.Uri;
import java.io.*;
import java.util.zip.*;
import android.graphics.Color;
import android.view.*;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;
import org.json.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Locale;

public class MainActivity extends Activity {
    static final double CORE=140.0;
    LinearLayout root; Spinner width,material; EditText denier,filament,kg; TextView result,info;
    double lastPred=0; JSONArray records;
    static final int EXPORT_XLSX=901;
    final String PREF="kutlucan_cal", KEY="records";

    @Override public void onCreate(Bundle b){super.onCreate(b); load(); build();}
    TextView tv(String s,int sp){TextView v=new TextView(this);v.setText(s);v.setTextColor(Color.WHITE);v.setTextSize(sp);v.setPadding(8,8,8,8);return v;}
    EditText edit(String hint){EditText e=new EditText(this);e.setHint(hint);e.setTextColor(Color.WHITE);e.setHintTextColor(0xff8fa9bf);e.setInputType(2|8192);e.setBackgroundColor(0xff17334b);e.setPadding(18,12,18,12);return e;}
    Button btn(String s){Button b=new Button(this);b.setText(s);return b;}
    void add(View v){root.addView(v,new LinearLayout.LayoutParams(-1,-2));}
    void build(){
      ScrollView sc=new ScrollView(this); root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(28,30,28,30);root.setBackgroundColor(0xff071b2e);sc.addView(root);
      TextView h=tv("KUTLUCAN TEKSTİL",24);h.setGravity(17);h.setTextColor(0xffffd166);add(h);
      TextView sub=tv("Ham İplik Bobin Çapı Hesaplama",19);sub.setGravity(17);add(sub);
      TextView ver=tv("Android v1.2 • EXE ile aynı hesaplama ve kalibrasyon modeli",12);ver.setGravity(17);ver.setTextColor(0xff9fb8ca);add(ver);

      add(tv("Masura genişliği",14)); width=new Spinner(this);width.setAdapter(new ArrayAdapter<String>(this,android.R.layout.simple_spinner_dropdown_item,new String[]{"150 mm","125 mm"}));add(width);
      add(tv("Materyal",14)); material=new Spinner(this);material.setAdapter(new ArrayAdapter<String>(this,android.R.layout.simple_spinner_dropdown_item,new String[]{"PA6","PA66","Diğer"}));add(material);
      denier=edit("Denye (örn. 70)"); add(denier); filament=edit("Filament (örn. 24)");add(filament);kg=edit("Net bobin kg (örn. 9.000)");add(kg);
      Button calc=btn("ÇAPI HESAPLA");calc.setOnClickListener(v->calculate());add(calc);
      result=tv("Tahmini çap: —",25);result.setGravity(17);result.setTextColor(0xffffd166);add(result);
      info=tv("Kalibrasyon kaydı: "+records.length(),13);info.setGravity(17);add(info);
      LinearLayout row=new LinearLayout(this);row.setOrientation(LinearLayout.HORIZONTAL);
      Button ok=btn("TUTARLI");Button no=btn("TUTARLI DEĞİL");row.addView(ok,new LinearLayout.LayoutParams(0,-2,1));row.addView(no,new LinearLayout.LayoutParams(0,-2,1));add(row);
      ok.setOnClickListener(v->feedback("Tutarlı"));no.setOnClickListener(v->feedback("Tutarlı Değil"));
      Button export=btn("TABLO ÇEK (EXCEL)");export.setOnClickListener(v->exportXlsx());add(export);
      Button manual=btn("MANUEL KALİBRASYON");manual.setOnClickListener(v->manual());add(manual);
      Button hist=btn("KALİBRASYON GEÇMİŞİ");hist.setOnClickListener(v->history());add(hist);
      TextView foot=tv("Faruk Tunçel • 21.09.2026 • Versiyon 1.2",12);foot.setGravity(17);foot.setTextColor(0xff8fa9bf);foot.setPadding(8,30,8,10);add(foot);
      setContentView(sc);
    }
    double val(EditText e){return Double.parseDouble(e.getText().toString().trim().replace(",","."));}
    int w(){return width.getSelectedItemPosition()==0?150:125;}
    void calculate(){try{
      double d=val(denier),f=val(filament),m=val(kg); if(d<=0||f<=0||m<=0)throw new Exception();
      double rho=estimate(d,f,m,w(),material.getSelectedItem().toString());
      lastPred=diameter(m,w(),rho);result.setText(String.format(Locale.US,"Tahmini dış çap: %.1f mm",lastPred));
      info.setText(String.format(Locale.US,"Model yoğunluğu: %.3f g/cm³   •   %d kalibrasyon",rho,records.length()));
    }catch(Exception e){toast("Denye, filament ve kg değerlerini kontrol edin.");}}
    double diameter(double m,int width,double rho){
      double dc=CORE/10.0,L=width/10.0,V=m*1000.0/rho;
      return Math.sqrt(dc*dc+4*V/(Math.PI*L))*10.0;
    }
    double density(double m,int width,double D){
      double dc=CORE/10.0,dd=D/10.0,L=width/10.0;
      double V=Math.PI/4.0*(dd*dd-dc*dc)*L;return m*1000.0/V;
    }
    double estimate(double d,double f,double m,int wi,String mat){
      double sw=0,s=0;
      for(int i=0;i<records.length();i++)try{
        JSONObject r=records.getJSONObject(i);double rd=r.getDouble("denier"),rf=r.getDouble("filament"),rm=r.getDouble("kg");
        int rw=r.getInt("width");String mt=r.getString("material");
        double dist=Math.abs(Math.log(rd/d))*1.55+Math.abs(Math.log(rf/f))*1.25+Math.abs(Math.log(rm/m))*0.35+(rw==wi?0:.80)+(mt.equals(mat)?0:.35);
        double wt=1.0/(.045+dist*dist);double rh=density(rm,rw,r.getDouble("diameter"));s+=wt*rh;sw+=wt;
      }catch(Exception ignored){}
      return sw>0?s/sw:.55;
    }
    void feedback(String source){
      if(lastPred<=0){toast("Önce çap hesabı yapın.");return;}
      LinearLayout box=new LinearLayout(this);box.setOrientation(LinearLayout.VERTICAL);box.setPadding(30,10,30,0);
      EditText ak=edit("Gerçek net kg");ak.setText(kg.getText());EditText ad=edit("Ölçülen gerçek dış çap (mm)");box.addView(ak);box.addView(ad);
      new AlertDialog.Builder(this).setTitle(source+" – Gerçek ölçüm").setView(box).setPositiveButton("Kalibre Et",(x,y)->{
        try{double m=val(ak),D=val(ad);double agreement=Math.max(0,100-Math.abs(lastPred-D)/D*100);addRecord(material.getSelectedItem().toString(),val(denier),val(filament),m,w(),D,source);
          new AlertDialog.Builder(this).setTitle("Kalibre edilmiştir").setMessage(String.format(Locale.US,"Tahmin: %.1f mm\\nGerçek: %.1f mm\\nTutarlılık: %%%.1f\\n\\nBu üretim bundan sonraki hesaplamalara dahil edildi.",lastPred,D,agreement)).setPositiveButton("Tamam",null).show();
        }catch(Exception e){toast("Gerçek kg ve çap değerlerini kontrol edin.");}
      }).setNegativeButton("İptal",null).show();
    }
    void manual(){
      LinearLayout b=new LinearLayout(this);b.setOrientation(LinearLayout.VERTICAL);b.setPadding(30,10,30,0);
      Spinner ww=new Spinner(this);ww.setAdapter(new ArrayAdapter<String>(this,android.R.layout.simple_spinner_dropdown_item,new String[]{"150 mm","125 mm"}));
      Spinner mm=new Spinner(this);mm.setAdapter(new ArrayAdapter<String>(this,android.R.layout.simple_spinner_dropdown_item,new String[]{"PA6","PA66","Diğer"}));
      EditText d=edit("Denye"),f=edit("Filament"),k=edit("Gerçek net kg"),dia=edit("Gerçek dış çap mm");b.addView(ww);b.addView(mm);b.addView(d);b.addView(f);b.addView(k);b.addView(dia);
      new AlertDialog.Builder(this).setTitle("Manuel kalibrasyon").setView(b).setPositiveButton("Kalibrasyona Dahil Et",(x,y)->{try{addRecord(mm.getSelectedItem().toString(),val(d),val(f),val(k),ww.getSelectedItemPosition()==0?150:125,val(dia),"Manuel");toast("Kalibre edilmiştir.");}catch(Exception e){toast("Değerleri kontrol edin.");}}).setNegativeButton("İptal",null).show();
    }
    void addRecord(String mat,double d,double f,double k,int wi,double dia,String src)throws Exception{
      JSONObject r=new JSONObject();r.put("time",new SimpleDateFormat("dd.MM.yyyy HH:mm:ss",Locale.getDefault()).format(new Date()));r.put("material",mat);r.put("denier",d);r.put("filament",f);r.put("kg",k);r.put("width",wi);r.put("diameter",dia);r.put("source",src);records.put(r);save();info.setText("Kalibrasyon kaydı: "+records.length());
    }
    void history(){StringBuilder s=new StringBuilder();for(int i=records.length()-1;i>=0&&i>=records.length()-30;i--)try{JSONObject r=records.getJSONObject(i);s.append(r.getString("time")).append(" • ").append(r.getString("material")).append(" • ").append(r.getDouble("denier")).append("/").append(r.getDouble("filament")).append(" • ").append(r.getInt("width")).append("mm • ").append(r.getDouble("kg")).append("kg • Ø").append(r.getDouble("diameter")).append("mm\\n\\n");}catch(Exception ignored){} new AlertDialog.Builder(this).setTitle("Kalibrasyon Geçmişi ("+records.length()+")").setMessage(s.toString()).setPositiveButton("Tamam",null).show();}
    void load(){try{String s=getSharedPreferences(PREF,0).getString(KEY,null);records=s==null?seeds():new JSONArray(s);}catch(Exception e){records=seeds();}}
    void save(){getSharedPreferences(PREF,0).edit().putString(KEY,records.toString()).apply();}
    JSONArray seeds(){JSONArray a=new JSONArray();try{
      seed(a,"PA6",70,24,4.500,150,286.0);seed(a,"PA6",20,24,6.775,150,348.2);seed(a,"PA6",70,68,9.000,125,425.0);seed(a,"PA6",40,34,9.000,150,402.0);seed(a,"PA6",40,34,4.553,150,299.0);
    }catch(Exception ignored){}return a;}
    void seed(JSONArray a,String mat,double d,double f,double k,int wi,double dia)throws Exception{JSONObject r=new JSONObject();r.put("time","Başlangıç");r.put("material",mat);r.put("denier",d);r.put("filament",f);r.put("kg",k);r.put("width",wi);r.put("diameter",dia);r.put("source","Gerçek üretim");a.put(r);}

    void exportXlsx(){
      Intent i=new Intent(Intent.ACTION_CREATE_DOCUMENT);i.addCategory(Intent.CATEGORY_OPENABLE);
      i.setType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
      i.putExtra(Intent.EXTRA_TITLE,"Kutlucan_Kalibrasyon_Tablosu_"+new SimpleDateFormat("yyyyMMdd_HHmm",Locale.US).format(new Date())+".xlsx");
      startActivityForResult(i,EXPORT_XLSX);
    }
    @Override protected void onActivityResult(int requestCode,int resultCode,Intent data){
      super.onActivityResult(requestCode,resultCode,data);
      if(requestCode==EXPORT_XLSX && resultCode==RESULT_OK && data!=null){
        try(OutputStream os=getContentResolver().openOutputStream(data.getData())){writeXlsx(os);toast("Excel tablosu kaydedildi.");}
        catch(Exception e){toast("Excel oluşturulamadı: "+e.getMessage());}
      }
    }
    String esc(String s){return s==null?"":s.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;").replace("\"","&quot;");}
    String cell(String ref,String v){return "<c r=\""+ref+"\" t=\"inlineStr\"><is><t>"+esc(v)+"</t></is></c>";}
    String num(String ref,double v){return "<c r=\""+ref+"\"><v>"+String.format(Locale.US,"%.6f",v)+"</v></c>";}
    void zip(ZipOutputStream z,String name,String text)throws Exception{z.putNextEntry(new ZipEntry(name));z.write(text.getBytes("UTF-8"));z.closeEntry();}
    void writeXlsx(OutputStream out)throws Exception{
      ZipOutputStream z=new ZipOutputStream(out);
      zip(z,"[Content_Types].xml","<?xml version=\"1.0\" encoding=\"UTF-8\"?><Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\"><Default Extension=\"rels\" ContentType=\"application/vnd.openxmlformats-package.relationships+xml\"/><Default Extension=\"xml\" ContentType=\"application/xml\"/><Override PartName=\"/xl/workbook.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml\"/><Override PartName=\"/xl/worksheets/sheet1.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml\"/></Types>");
      zip(z,"_rels/.rels","<?xml version=\"1.0\" encoding=\"UTF-8\"?><Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\"><Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument\" Target=\"xl/workbook.xml\"/></Relationships>");
      zip(z,"xl/workbook.xml","<?xml version=\"1.0\" encoding=\"UTF-8\"?><workbook xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\" xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\"><sheets><sheet name=\"Kalibrasyonlar\" sheetId=\"1\" r:id=\"rId1\"/></sheets></workbook>");
      zip(z,"xl/_rels/workbook.xml.rels","<?xml version=\"1.0\" encoding=\"UTF-8\"?><Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\"><Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet\" Target=\"worksheets/sheet1.xml\"/></Relationships>");
      StringBuilder s=new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\"?><worksheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\"><sheetData>");
      String[] h={"Tarih Saat","Materyal","Denye","Filament","Masura Genişliği mm","Çekirdek Çapı mm","Gerçek Net kg","Gerçek Dış Çap mm","Efektif Yoğunluk g/cm3","Kaynak"};
      s.append("<row r=\"1\">");for(int j=0;j<h.length;j++)s.append(cell(Character.toString((char)('A'+j))+"1",h[j]));s.append("</row>");
      for(int i=0;i<records.length();i++){JSONObject r=records.getJSONObject(i);int n=i+2;String N=""+n;
        s.append("<row r=\"").append(n).append("\">").append(cell("A"+N,r.optString("time"))).append(cell("B"+N,r.optString("material")))
        .append(num("C"+N,r.getDouble("denier"))).append(num("D"+N,r.getDouble("filament"))).append(num("E"+N,r.getInt("width"))).append(num("F"+N,CORE))
        .append(num("G"+N,r.getDouble("kg"))).append(num("H"+N,r.getDouble("diameter"))).append(num("I"+N,density(r.getDouble("kg"),r.getInt("width"),r.getDouble("diameter"))))
        .append(cell("J"+N,r.optString("source"))).append("</row>");
      }
      s.append("</sheetData></worksheet>");zip(z,"xl/worksheets/sheet1.xml",s.toString());z.finish();z.close();
    }

    void toast(String s){Toast.makeText(this,s,Toast.LENGTH_LONG).show();}
}