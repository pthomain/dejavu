package uk.co.glass_software.android.cache_interceptor.demo;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ExpandableListView;
import android.widget.RadioButton;

import uk.co.glass_software.android.cache_interceptor.demo.retrofit.RetrofitDemoPresenter;
import uk.co.glass_software.android.cache_interceptor.demo.volley.VolleyDemoPresenter;

public class DemoActivity extends AppCompatActivity {
    
    private Method method = Method.RETROFIT;
    
    private EditText editText;
    private Button button;
    private DemoPresenter retrofitDemoPresenter;
    private DemoPresenter volleyDemoPresenter;
    private ExpandableListAdapter listAdapter;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        editText = findViewById(R.id.text_input);
        ExpandableListView result = findViewById(R.id.result);
        button = findViewById(R.id.load_button);
        button.setOnClickListener(ignore -> onButtonClick());
        
        RadioButton retrofitRadioButton = findViewById(R.id.radio_button_retrofit);
        RadioButton volleyRadioButton = findViewById(R.id.radio_button_volley);
        
        retrofitRadioButton.setOnClickListener(ignore -> method = Method.RETROFIT);
        volleyRadioButton.setOnClickListener(ignore -> method = Method.VOLLEY);
        
        listAdapter = new ExpandableListAdapter(
                this,
                () -> button.setEnabled(true)
        );
        
        retrofitDemoPresenter = new RetrofitDemoPresenter(this, listAdapter::log);
        volleyDemoPresenter = new VolleyDemoPresenter(this, listAdapter::log);
        
        result.setAdapter(listAdapter);
        listAdapter.notifyDataSetChanged();
    }
    
    private void onButtonClick() {
        String location = editText.getText().toString();
        if (!location.isEmpty()) {
            button.setEnabled(false);
            listAdapter.loadCity(getDemoPresenter().loadResponse(location));
        }
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
