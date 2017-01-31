package com.example.cloudlet.ingunibo.cloudlettestapp;

import android.app.Activity;
import android.app.ListActivity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import java.util.ArrayList;

public class HandoffListActivity extends ListActivity {
    private static final String TAG = "HandoffListActivity";
    private static String INTENT_HANDOFF_IP_EXTRA_NAME = "EXTRA_LOCAL_IP";

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent hoListIntent = getIntent();
        ArrayList<String> ipaddrs = hoListIntent.getStringArrayListExtra(INTENT_HANDOFF_IP_EXTRA_NAME);
        ArrayAdapter myIPadapter = new ArrayAdapter</*String*/>(this, android.R.layout.simple_list_item_1, ipaddrs);
        setListAdapter(myIPadapter);
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        Log.i(TAG, "Clicked item in position "+position);
        ListView myIPList = getListView();
        String ipSelected = myIPList.getAdapter().getItem(position).toString();

        Intent intentResult = new Intent();
        intentResult.putExtra(INTENT_HANDOFF_IP_EXTRA_NAME, ipSelected);
        setResult(Activity.RESULT_OK, intentResult);
        finish();
    }
}
