package com.github.catvod.spider;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.text.TextUtils;

import com.github.catvod.bean.Result;
import com.github.catvod.bean.Vod;
import com.github.catvod.crawler.Spider;
import com.github.catvod.utils.MyDataBaseHelper;
import com.github.catvod.utils.Notify;
import com.github.catvod.utils.Path;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class Iktv extends Spider {

    private SQLiteOpenHelper dataBaseHelper;

    private SQLiteDatabase db;

    @Override
    public void init(Context context, String extend) throws Exception {
        super.init(context, extend);
        try {
            String dbPath = Path.download().getAbsolutePath() + File.separator + "song.db";
            File dbFile = new File(Path.download().getAbsolutePath(), "song.db");
            if (!dbFile.exists()) {
                Notify.show("请下载 song.db 文件至手机的Download目录下");
                Init.run(() -> {
                    //downloadSongDB();
                });
            } else {
                dataBaseHelper = new SQLiteOpenHelper(context, dbPath, null, 1) {
                    @Override
                    public void onCreate(SQLiteDatabase db) {

                    }

                    @Override
                    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

                    }
                };
                db = dataBaseHelper.getReadableDatabase();
            }
        } catch (Exception e) {

        }
    }

    @Override
    public String homeContent(boolean filter) throws Exception {
        JSONArray classes = new JSONArray();
        List<String> typeIds = Arrays.asList("1", "2");
        List<String> typeNames = Arrays.asList("歌手", "曲库");
        for (int i = 0; i < typeIds.size(); i++) {
            JSONObject c = new JSONObject();
            c.put("type_id", typeIds.get(i));
            c.put("type_name", typeNames.get(i));
            classes.put(c);
        }
        JSONObject result = new JSONObject()
                .put("class", classes);
        return result.toString();
    }

    private List<Vod> getList(String tid, Integer page) {
        List<Vod> list = new ArrayList<>();
        if (tid.equals("1")) {
            // 查询数据
            String[] projection = { "id", "name" };
            String limit = 20*(page-1) + ",20";
            //String selection = "name = ?";
            //String[] selectionArgs = { "John" };
            Cursor cursor = db.query("singer", projection, null, null, null, null, "id", limit);

            // 遍历查询结果
            while (cursor.moveToNext()) {
                int id = cursor.getInt(cursor.getColumnIndexOrThrow("id"));
                String name = cursor.getString(cursor.getColumnIndexOrThrow("name"));
                // 处理查询到的数据
                list.add(new Vod(name, name, "http://txysong.mysoto.cc/songs/" + id + ".jpg", ""));
            }
            // 关闭数据库连接
            cursor.close();
        } else {
            // 查询数据
            String[] projection = { "number", "name" };
            String limit = 20*(page-1) + ",20";
            //String selection = "name = ?";
            //String[] selectionArgs = { "John" };
            Cursor cursor = db.query("song", projection, null, null, null, null, "number", limit);

            // 遍历查询结果
            while (cursor.moveToNext()) {
                String id = cursor.getString(cursor.getColumnIndexOrThrow("number"));
                String name = cursor.getString(cursor.getColumnIndexOrThrow("name"));
                // 处理查询到的数据
                list.add(new Vod("http://txysong.mysoto.cc/songs/" + id + ".mkv", name, null, null));
            }
            // 关闭数据库连接
            cursor.close();
        }
        return list;
    }

    @Override
    public String categoryContent(String tid, String pg, boolean filter, HashMap<String, String> extend) throws Exception {
        if (tid.endsWith("_onclick")) return searchContent(tid.split("_")[0], false, pg);
        int page = Integer.parseInt(pg);
        List<Vod> list = getList(tid, page);
        return  Result.string(list);
    }

    @Override
    public String detailContent(List<String> ids) throws Exception {
        Vod vod = new Vod();
        vod.setVodId(ids.get(0));
        vod.setVodName(ids.get(0));
        vod.setVodPlayFrom("Leospring");
        if (ids.get(0).endsWith(".mkv")) {
            vod.setVodPlayUrl("播放$" + ids.get(0));
        } else {
            // 查询数据
            vod.setVodActor(getLink(ids.get(0)));
            String[] projection = { "number", "name" };
            String limit = "0,999";
            String selection = "singer_names = ?";
            String[] selectionArgs = { ids.get(0) };
            Cursor cursor = db.query("song", projection, selection, selectionArgs, null, null, "number", limit);
            List<String> list = new ArrayList<>();
            // 遍历查询结果
            while (cursor.moveToNext()) {
                String id = cursor.getString(cursor.getColumnIndexOrThrow("number"));
                String name = cursor.getString(cursor.getColumnIndexOrThrow("name"));
                // 处理查询到的数据
                list.add(name + "$http://txysong.mysoto.cc/songs/" + id + ".mkv");
            }
            // 关闭数据库连接
            cursor.close();
            vod.setVodPlayUrl(TextUtils.join("#", list));
        }
        return Result.string(vod);
    }

    @Override
    public String playerContent(String flag, String id, List<String> vipFlags) throws Exception {
        return Result.get().url(id).string();
    }

    @Override
    public String searchContent(String key, boolean quick, String pg) throws Exception {
        // 查询数据
        String[] projection = { "number", "name" };
        String limit = "0,999";
        String selection = "name LIKE ? OR singer_names LIKE ?";
        String[] selectionArgs = { "%"+key+"%", "%"+key+"%"};
        Cursor cursor = db.query("song", projection, selection, selectionArgs, null, null, null, null);
        List<Vod> list = new ArrayList<>();
        // 遍历查询结果
        while (cursor.moveToNext()) {
            String id = cursor.getString(cursor.getColumnIndexOrThrow("number"));
            String name = cursor.getString(cursor.getColumnIndexOrThrow("name"));
            // 处理查询到的数据
            list.add(new Vod("http://txysong.mysoto.cc/songs/" + id + ".mkv", name,null,null));
        }
        // 关闭数据库连接
        cursor.close();
        return Result.string(list);
    }

    @Override
    public void destroy() {
        db.close();
    }

    private String getLink(String title) {
        return String.format("[a=cr:{\"id\":\"%s\",\"name\":\"%s\"}/]%s[/a]", title + "_onclick", title, title);
    }
}
