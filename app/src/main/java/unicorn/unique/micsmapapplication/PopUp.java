package unicorn.unique.micsmapapplication;

import android.app.Activity;
import android.support.v4.view.ScrollingView;
import android.widget.ScrollView;
import android.widget.TextView;
import android.util.DisplayMetrics;
import android.os.Bundle;
import android.content.Intent;
import android.util.Log;

import java.util.ArrayList;


/**
 * Created by HolySith on 26-Dec-16.
 */

public class PopUp extends Activity implements AsyncResponse {

    AsyncRealTimeBus asyncRealTimeBus = new AsyncRealTimeBus();
    TextView tv;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.popupwindow);
        Intent intent = getIntent();
        int stationId = intent.getIntExtra("stationID",-1);

        DisplayMetrics dm = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(dm);

        int width = dm.widthPixels;
        int height = dm.heightPixels;

        getWindow().setLayout((int)(width*0.8), (int)(height*0.7));

        String dataAddress= "http://travelplanner.mobiliteit.lu/restproxy/departureBoard?accessId=cdt&id=A=1@L="+stationId+"&format=json";

        asyncRealTimeBus.delegate = this;
        asyncRealTimeBus.execute(dataAddress);

    }

    @Override
    public void listProcessFinish(ArrayList<stationLocations> out) {
    }

    @Override
    public void processFinish(String output) {
        TextView textView = (TextView) findViewById(R.id.busText);
        textView.setText(output);
    }
}
