package uk.co.glass_software.android.cache_interceptor.demo;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.EditText;
import android.widget.TextView;

import uk.co.glass_software.android.cache_interceptor.utils.SimpleLogger;

public class MainActivity extends AppCompatActivity {
    
    private SimpleLogger simpleLogger;
    private TextView resultText;
    private MainPresenter presenter;
    private EditText editText;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        simpleLogger = new SimpleLogger(this);
        presenter = new MainPresenter(this, this::appendText);
        
        editText = (EditText) findViewById(R.id.text_input);
        resultText = ((TextView) findViewById(R.id.result));
        findViewById(R.id.load_button).setOnClickListener(ignore -> onButtonClick());
    }
    
    private void onButtonClick() {
        String location = editText.getText().toString();
        
        if (!location.isEmpty()) {
            resultText.setText("");
            long start = System.currentTimeMillis();
            presenter.loadResponse(
                    location,
                    () -> appendText("Loading\n\n"),
                    this::appendText,
                    () -> appendText("\n\nEnd of request: " + (System.currentTimeMillis() - start) + "ms\n\n")
            );
        }
    }
    
    private void appendText(String output) {
        resultText.post(() -> resultText.setText(resultText.getText() + simpleLogger.prettyPrint(output) + "\n"));
    }
    
}
