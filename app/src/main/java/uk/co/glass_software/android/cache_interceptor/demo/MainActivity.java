package uk.co.glass_software.android.cache_interceptor.demo;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {
    
    private TextView resultText;
    private MainPresenter presenter;
    private EditText editText;
    private Button button;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        presenter = new MainPresenter(this, this::appendText);
        
        editText = (EditText) findViewById(R.id.text_input);
        resultText = ((TextView) findViewById(R.id.result));
        button = (Button) findViewById(R.id.load_button);
        button.setOnClickListener(ignore -> onButtonClick());
    }
    
    private void onButtonClick() {
        String location = editText.getText().toString();
        
        if (!location.isEmpty()) {
            resultText.setText("");
            long start = System.currentTimeMillis();
            button.setEnabled(false);
            appendText("Loading\n");
            
            presenter.loadResponse(
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
                                                 + presenter.prettyPrint(output)
        ));
    }
    
}
