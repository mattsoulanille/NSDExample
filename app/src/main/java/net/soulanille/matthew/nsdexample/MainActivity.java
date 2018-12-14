package net.soulanille.matthew.nsdexample;
// https://stackoverflow.com/questions/5070830/populating-a-listview-using-an-arraylist
import android.content.Context;
import android.database.DataSetObserver;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import java.io.IOException;

public class MainActivity extends AppCompatActivity {

    private ListView peerView;
    private NsdHelper mNsdHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Context context = getApplicationContext();

        peerView = (ListView) findViewById(R.id.peerView);


        try {
            mNsdHelper = new NsdHelper(context);

            ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(
                    this,
                    android.R.layout.simple_list_item_1,
                    mNsdHelper.getPeerList()
            );

            mNsdHelper.registerObserver(new DataSetObserver() {
                public void onChanged() {
                    runOnUiThread(new Runnable() {
                        // https://stackoverflow.com/questions/5161951/android-only-the-original-thread-that-created-a-view-hierarchy-can-touch-its-vi
                        @Override
                        public void run() {
                            arrayAdapter.notifyDataSetChanged();
                        }
                    });
                }
            });

            peerView.setAdapter(arrayAdapter);



        }
        catch(IOException e) {
            Log.e("MainActivity", "Got IOException " + e.getMessage());
        }
    }

    @Override
    protected void onPause() {
        if (mNsdHelper != null) {
            mNsdHelper.tearDown();
        }
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mNsdHelper != null) {
            // Re-register the service
            mNsdHelper.registerService(mNsdHelper.mLocalPort);
            mNsdHelper.discoverServices();
        }
    }

    @Override
    protected void onDestroy() {
        if (mNsdHelper != null) {
            mNsdHelper.tearDown();
            // Also tear down the connection here if you have one.
        }
        super.onDestroy();
    }
}
