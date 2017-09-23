package uk.co.glass_software.android.cache_interceptor.demo;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.TextView;

import uk.co.glass_software.android.cache_interceptor.demo.retrofit.RetrofitDemoPresenter;
import uk.co.glass_software.android.cache_interceptor.demo.volley.VolleyDemoPresenter;

public class DemoActivity extends AppCompatActivity {
    
    private Method method = Method.RETROFIT;
    
    private TextView resultText;
    private EditText editText;
    private Button button;
    private DemoPresenter retrofitDemoPresenter;
    private DemoPresenter volleyDemoPresenter;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        editText = findViewById(R.id.text_input);
        resultText = findViewById(R.id.result);
        button = findViewById(R.id.load_button);
        button.setOnClickListener(ignore -> onButtonClick());
        
        RadioButton retrofitRadioButton = findViewById(R.id.radio_button_retrofit);
        RadioButton volleyRadioButton = findViewById(R.id.radio_button_volley);
        
        retrofitRadioButton.setOnClickListener(ignore -> method = Method.RETROFIT);
        volleyRadioButton.setOnClickListener(ignore -> method = Method.VOLLEY);
        
        retrofitDemoPresenter = new RetrofitDemoPresenter(this, this::appendText);
        volleyDemoPresenter = new VolleyDemoPresenter(this, this::appendText);
    }
    
    private void onButtonClick() {
        String location = editText.getText().toString();
        
        if (!location.isEmpty()) {
            resultText.setText("");
            long start = System.currentTimeMillis();
            button.setEnabled(false);
            appendText(String.format("Loading request using %s\n", method));
            
            getDemoPresenter().loadResponse(
                    location,
                    this::appendText,
                    () -> {
                        appendText("\n\nEnd of request: " + (System.currentTimeMillis() - start) + "ms\n\n");
                        button.post(() -> button.setEnabled(true));
                    }
            );
        }
    }
    
    private void appendText(String output) {
        resultText.post(() -> resultText.setText(resultText.getText()
                                                 + "\n"
                                                 + output
        ));
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
