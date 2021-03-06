package com.imaginat.androidtodolist.businessModels;

import android.content.Context;
import android.database.Cursor;
import android.os.AsyncTask;
import android.util.Log;

import com.imaginat.androidtodolist.data.DbSchema;
import com.imaginat.androidtodolist.data.ToDoListSQLHelper;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

/**
 * Created by nat on 5/10/16.
 */
public class ListManager implements TheDeleter.IUseTheDeleter {

    private static final String TAG = ListManager.class.getSimpleName();
    public static ListManager instance;
    private ToDoListSQLHelper mSqlHelper;
    private ArrayList<ListTitle> mListTitles;

    private JSONArray mListJSONArray, mRemindersArray;


    private ListManager(Context context) {
        mSqlHelper = ToDoListSQLHelper.getInstance(context);
        mListTitles = new ArrayList<ListTitle>();
    }


    public static ListManager getInstance(Context context) {
        if (instance == null) {
            instance = new ListManager(context);
        }

        return instance;
    }

    public ArrayList<ListTitle> getListTitles() {
        return mListTitles;
    }

    /**
     * @param listName
     * @return - row id as string
     */
    public String createNewList(String listName) {
        //check if list name already exists
        int countFound = mSqlHelper.doesListNameExist(listName);

        //if yes, modify list name
        if (countFound > 0) {
            Log.d(TAG, "createNewList found countFoud " + countFound);
            listName += "(" + countFound + ")";
        } else {
            Log.d(TAG, "created new list NOT found");
        }

        //add it
        return mSqlHelper.insertIntoListTable(listName);

    }

    public void updateAllListTitles() {
        Cursor c = mSqlHelper.getAllListNames();
        mListTitles.clear();
        c.moveToFirst();
        while (c.isAfterLast() == false) {
            int colIndex = c.getColumnIndex(DbSchema.lists_table.cols.LIST_ID);
            int list_id = c.getInt(colIndex);
            colIndex = c.getColumnIndex(DbSchema.lists_table.cols.LIST_TITLE);
            String title = c.getString(colIndex);
            ListTitle lt = new ListTitle();
            lt.setText(title);
            lt.setList_id(Integer.toString(list_id));
            mListTitles.add(lt);
            c.moveToNext();
        }
    }

    public void readListJSON(JSONArray allListsJSON, JSONArray allReminders, Context context) {

        mListJSONArray = allListsJSON;
        mRemindersArray = allReminders;
        TheDeleter deleter = new TheDeleter(context);
        deleter.execute("ALL");
        deleter.setDeletionCompleteCallback(this);

    }

    @Override
    public void deletionCompleted(boolean result, String s) {
        int total = mListJSONArray.length();

        try {
            ArrayList<ListTitle> listTitles = new ArrayList<>();
            for (int i = 0; i < total; i++) {
                ListTitle lt = new ListTitle();
                JSONObject j = mListJSONArray.getJSONObject(i);
                String text = j.getString("list_title");
                String id = j.getString("list_id");
                lt.setText(text);
                lt.setList_id(id);
                listTitles.add(lt);
            }

            PopulateListTask populateListTask = new PopulateListTask();
            populateListTask.execute(listTitles);
        } catch (JSONException jse) {

        } catch (Exception ex) {

        }
    }

    public class PopulateListTask extends AsyncTask<ArrayList<ListTitle>, Void, Boolean> {


        @Override
        protected Boolean doInBackground(ArrayList<ListTitle>... params) {
            mSqlHelper.insertMultipleListValues(params[0]);
            return null;
        }

        @Override
        protected void onPostExecute(Boolean aBoolean) {
            super.onPostExecute(aBoolean);

            //read in the json
            int total = mRemindersArray.length();
            ArrayList<ToDoListItem> reminders = new ArrayList<>();
            try {

                for (int i = 0; i < total; i++) {
                    ToDoListItem todoListItem = new ToDoListItem();
                    JSONObject j = mRemindersArray.getJSONObject(i);
                    String text = j.getString("text");
                    String listID = j.getString("listID");
                    String reminderID=j.getString("reminderID");
                    todoListItem.setListId(listID);
                    todoListItem.setReminder_id(reminderID);
                    todoListItem.setText(text);
                    reminders.add(todoListItem);
                }
            } catch (JSONException jse) {

            } catch (Exception ex) {

            }

            PopulateReminders populateReminders = new PopulateReminders();
            populateReminders.execute(reminders);
        }
    }

    public class PopulateReminders extends AsyncTask<ArrayList<ToDoListItem>, Void, Boolean> {

        @Override
        protected Boolean doInBackground(ArrayList<ToDoListItem>... params) {

            mSqlHelper.insertMultipleReminderValues(params[0]);
            return true;
        }
    }
}
