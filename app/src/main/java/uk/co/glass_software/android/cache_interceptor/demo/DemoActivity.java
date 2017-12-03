package uk.co.glass_software.android.cache_interceptor.demo;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.Button;
import android.widget.ExpandableListView;
import android.widget.RadioButton;
import android.widget.TextView;

import uk.co.glass_software.android.cache_interceptor.demo.retrofit.RetrofitDemoPresenter;
import uk.co.glass_software.android.cache_interceptor.demo.volley.VolleyDemoPresenter;

public class DemoActivity extends AppCompatActivity {
    
    private Method method = Method.RETROFIT;
    
    private Button loadButton;
    private Button refreshButton;
    private DemoPresenter retrofitDemoPresenter;
    private DemoPresenter volleyDemoPresenter;
    private ExpandableListAdapter listAdapter;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        ExpandableListView result = findViewById(R.id.result);
        loadButton = findViewById(R.id.load_button);
        refreshButton = findViewById(R.id.refresh_button);
        loadButton.setOnClickListener(ignore -> onLoadButtonClick());
        refreshButton.setOnClickListener(ignore -> onRefreshButtonClick());
        
        RadioButton retrofitRadioButton = findViewById(R.id.radio_button_retrofit);
        RadioButton volleyRadioButton = findViewById(R.id.radio_button_volley);
        
        retrofitRadioButton.setOnClickListener(ignore -> method = Method.RETROFIT);
        volleyRadioButton.setOnClickListener(ignore -> method = Method.VOLLEY);
        
        TextView jokeView = findViewById(R.id.joke);
        listAdapter = new ExpandableListAdapter(
                this,
                jokeView::setText,
                () -> setButtonsEnabled(true)
        );
        
        retrofitDemoPresenter = new RetrofitDemoPresenter(this, listAdapter::log);
        volleyDemoPresenter = new VolleyDemoPresenter(this, listAdapter::log);
        
        result.setAdapter(listAdapter);
        listAdapter.notifyDataSetChanged();
    }
    
    private void onRefreshButtonClick() {
        setButtonsEnabled(false);
        listAdapter.loadJoke(getDemoPresenter().loadResponse(true));
    }
    
    private void onLoadButtonClick() {
        setButtonsEnabled(false);
        listAdapter.loadJoke(getDemoPresenter().loadResponse(false));
    }
    
    private void setButtonsEnabled(boolean isEnabled) {
        loadButton.setEnabled(isEnabled);
        refreshButton.setEnabled(isEnabled);
    }
    
    public DemoPresenter getDemoPresenter() {
        switch (method) {
            case RETROFIT:
                return retrofitDemoPresenter;
            
            case VOLLEY:
                return volleyDemoPresenter;
        }
        
        throw new IllegalStateException("Unknown method: " + method);
    }
    
    private enum Method {
        RETROFIT,
        VOLLEY
    }
}
