package com.github.catvod.spider;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.text.TextUtils;

import com.github.catvod.bean.Result;
import com.github.catvod.bean.Vod;
import com.github.catvod.crawler.Spider;
import com.github.catvod.utils.Notify;
import com.github.catvod.utils.Path;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

// 定义一个继承自Spider类的自定义类
public class DySpider extends Spider {

    private SQLiteOpenHelper dataBaseHelper; // SQLiteOpenHelper实例，用于管理数据库连接
    private SQLiteDatabase db; // SQLiteDatabase实例，用于执行数据库操作

    @Override
    public void init(Context context, String extend) throws Exception {
        super.init(context, extend); // 调用父类的初始化方法
        try {
            // 设置数据库路径
            String dbPath = Path.download().getAbsolutePath() + File.separator + "dy.db";
            File dbFile = new File(dbPath);
            if (!dbFile.exists()) { // 检查数据库文件是否存在
                Notify.show("请下载 dy.db 文件至手机的Download目录下");
            } else {
                // 初始化SQLiteOpenHelper
                dataBaseHelper = new SQLiteOpenHelper(context, dbPath, null, 1) {
                    @Override
                    public void onCreate(SQLiteDatabase db) {}

                    @Override
                    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {}
                };
                db = dataBaseHelper.getReadableDatabase(); // 获取可读数据库
            }
        } catch (Exception e) {
            e.printStackTrace(); // 捕获并打印异常
        }
    }

    @Override
    public String homeContent(boolean filter) throws Exception {
        JSONArray classes = new JSONArray(); // 创建一个JSONArray对象，用于存储分类信息
        Cursor cursor = db.query("dyfl", new String[]{"type_id", "type_name"}, null, null, null, null, null); // 查询dyfl表中的type_id和type_name

        while (cursor.moveToNext()) { // 遍历查询结果
            JSONObject c = new JSONObject();
            c.put("type_id", cursor.getString(cursor.getColumnIndexOrThrow("type_id"))); // 获取type_id
            c.put("type_name", cursor.getString(cursor.getColumnIndexOrThrow("type_name"))); // 获取type_name
            classes.put(c); // 将分类信息添加到JSONArray中
        }
        cursor.close(); // 关闭游标

        JSONObject result = new JSONObject().put("class", classes); // 创建结果JSON对象
        return result.toString(); // 返回结果字符串
    }

    // 获取视频列表
    private List<Vod> getList(String tid, Integer page) {
        List<Vod> list = new ArrayList<>();
        String limit = 20 * (page - 1) + ",20"; // 设置分页限制
        Cursor cursor = db.query("vod", new String[]{"vod_id", "vod_name", "vod_pic"}, "type_id=?", new String[]{tid}, null, null, "vod_id", limit); // 查询vod表

        while (cursor.moveToNext()) { // 遍历查询结果
            String id = cursor.getString(cursor.getColumnIndexOrThrow("vod_id"));
            String name = cursor.getString(cursor.getColumnIndexOrThrow("vod_name"));
            String pic = cursor.getString(cursor.getColumnIndexOrThrow("vod_pic"));
            list.add(new Vod(id, name, pic, "")); // 将结果添加到列表中
        }
        cursor.close(); // 关闭游标
        return list;
    }

    @Override
    public String categoryContent(String tid, String pg, boolean filter, HashMap<String, String> extend) throws Exception {
        int page = Integer.parseInt(pg); // 将页面字符串转换为整数
        List<Vod> list = getList(tid, page); // 获取视频列表
        return Result.string(list); // 返回结果字符串
    }

    @Override
    public String detailContent(List<String> ids) throws Exception {
        Vod vod = new Vod(); // 创建Vod实例
        String id = ids.get(0);
        Cursor cursor = db.query("vod", new String[]{"vod_id", "vod_name", "vod_pic", "vod_play_url"}, "vod_id=?", new String[]{id}, null, null, null); // 查询视频详情

        if (cursor.moveToFirst()) { // 检查是否有结果
            vod.setVodId(cursor.getString(cursor.getColumnIndexOrThrow("vod_id")));
            vod.setVodName(cursor.getString(cursor.getColumnIndexOrThrow("vod_name")));
            vod.setVodPic(cursor.getString(cursor.getColumnIndexOrThrow("vod_pic")));
            vod.setVodPlayUrl(cursor.getString(cursor.getColumnIndexOrThrow("vod_play_url")));
        }
        cursor.close(); // 关闭游标
        return Result.string(vod); // 返回结果字符串
    }

    @Override
    public String playerContent(String flag, String id, List<String> vipFlags) throws Exception {
        return Result.get().url(id).string(); // 返回视频播放URL
    }

    @Override
    public String searchContent(String key, boolean quick, String pg) throws Exception {
        String[] projection = {"vod_id", "vod_name", "vod_pic"};
        String selection = "vod_name LIKE ?";
        String[] selectionArgs = {"%" + key + "%"};
        Cursor cursor = db.query("vod", projection, selection, selectionArgs, null, null, null); // 根据关键字搜索视频
        List<Vod> list = new ArrayList<>();

        while (cursor.moveToNext()) { // 遍历查询结果
            String id = cursor.getString(cursor.getColumnIndexOrThrow("vod_id"));
            String name = cursor.getString(cursor.getColumnIndexOrThrow("vod_name"));
            String pic = cursor.getString(cursor.getColumnIndexOrThrow("vod_pic"));
            list.add(new Vod(id, name, pic, "")); // 将结果添加到列表中
        }
        cursor.close(); // 关闭游标
        return Result.string(list); // 返回结果字符串
    }

    @Override
    public void destroy() {
        db.close(); // 关闭数据库连接
    }
}