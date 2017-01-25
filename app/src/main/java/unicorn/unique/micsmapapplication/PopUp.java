package unicorn.unique.micsmapapplication;

import android.app.Activity;
import android.graphics.Typeface;
import android.support.v4.view.ScrollingView;
import android.text.Html;
import android.text.SpannableStringBuilder;
import android.text.style.StyleSpan;
import android.widget.ScrollView;
import android.widget.TextView;
import android.util.DisplayMetrics;
import android.os.Bundle;
import android.content.Intent;
import android.util.Log;

import java.util.ArrayList;
import java.util.Locale;


/**
 * Created by HolySith on 26-Dec-16.
 */

public class PopUp extends Activity implements AsyncResponse {

    AsyncRealTimeBus asyncRealTimeBus = new AsyncRealTimeBus();
    AsyncRealTimeVeloh asyncRealTimeVeloh = new AsyncRealTimeVeloh();
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


        String dataAddress;
        if(stationId>1000) {
            getWindow().setLayout((int)(width*0.8), (int)(height*0.5));
            dataAddress = "http://travelplanner.mobiliteit.lu/restproxy/departureBoard?accessId=cdt&id=A=1@L=" + stationId + "&format=json";
            asyncRealTimeBus.delegate = this;
            asyncRealTimeBus.execute(dataAddress);
        }else{
            getWindow().setLayout((int)(width*0.8), (int)(height*0.2));
            dataAddress = "https://api.jcdecaux.com/vls/v1/stations?contract=Luxembourg&apiKey=d3369fd018b460c87544a5f04f0937a41e669a47";
            asyncRealTimeVeloh.delegate = this;
            asyncRealTimeVeloh.execute(dataAddress,Integer.toString(stationId));

        }


    }

    @Override
    public void listProcessFinish(ArrayList<stationLocations> out) {
    }

    @Override
    public void velohlistProcessFinish(ArrayList<stationLocations> out) {

    }

    @Override
    public void velohRTProcessFinish(String output) {
        TextView textView = (TextView) findViewById(R.id.busText);
        if(output!=null) {
            textView.setText(Html.fromHtml(output));
        }else{
            output ="No data to display.";
            textView.setText(output);
        }
    }

    @Override
    public void processFinish(String output) {
        TextView textView = (TextView) findViewById(R.id.busText);
        if(output!=null) {
            textView.setText(Html.fromHtml(output));
        }else{
            output ="No data to display.";
            textView.setText(output);
        }
    }

}
