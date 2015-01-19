package ts.test;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.json.JSONException;
import org.json.JSONObject;

import ts.test.StartActivity.HttpGetTask;

import android.app.Activity;
import android.content.Intent;
import android.net.http.AndroidHttpClient;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {
	private static final String LOCAL_FILE = "key.txt";
	private static final String TAG = "MainActivity";
	
	public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //setContentView(R.layout.activity_main);

        Intent intent = getIntent();
        String action = intent.getAction();
        
      //ファイルの読み込み
        InputStream in;
        String lineBuffer ="";
        try {
            in = openFileInput(LOCAL_FILE); 
     
            BufferedReader reader= new BufferedReader(new InputStreamReader(in,"UTF-8"));
            while( (lineBuffer = reader.readLine()) != null ){
                Log.d("FileAccess",lineBuffer);
            }
        } catch (IOException e) {
            // TODO 自動生成された catch ブロック
            e.printStackTrace();
        }
        
        if(action.equals((NfcAdapter.ACTION_NDEF_DISCOVERED))) {
        	byte[] rawId = intent.getByteArrayExtra(NfcAdapter.EXTRA_ID);
        	String text = bytesToText(rawId);
        	//TextView nfcIdTextView = (TextView)findViewById(R.id.nfc_id_textview);
        	//nfcIdTextView.setText(text);
        	
        	Parcelable[] messages = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);
        	NdefMessage message = (NdefMessage)messages[0];
        	NdefRecord[] records = message.getRecords();
        	String textAndLangCode ="";
        	for(NdefRecord record : records) {
        		if (isTextRecord(record)) {
        			textAndLangCode = getTextAndLangCode(record);
        			} else {
        			Toast.makeText(getApplicationContext(), "Not Text Record", Toast.LENGTH_SHORT)
        			.show();
        			}
        	}
            Log.d("TEST", text);
            //Toast.makeText(this, text, Toast.LENGTH_LONG).show();
            //TextView txtResult = (TextView)findViewById(R.id.msg);
            //txtResult.setText(textAndLangCode);
            if(textAndLangCode.startsWith("start")){
            	HttpPost request = new HttpPost("https://ec2-54-65-77-17.ap-northeast-1.compute.amazonaws.com:3000/api/1.0/attendance_time");
    			request.setHeader("Authorization", "Bearer ?" + lineBuffer +"?");
    			new HttpGetTask().execute(request);

    			Toast.makeText(this, "出勤しました", Toast.LENGTH_LONG).show();
            }else if(textAndLangCode.startsWith("end")){
            	HttpPost request = new HttpPost("https://ec2-54-65-77-17.ap-northeast-1.compute.amazonaws.com:3000/api/1.0/quitting_time");
    			request.setHeader("Authorization", "Bearer ?" + lineBuffer +"?");
    			new HttpGetTask().execute(request);
    			
    			Toast.makeText(this, "退勤しました", Toast.LENGTH_LONG).show();
            }else{
            	Toast.makeText(this, "システムエラー", Toast.LENGTH_LONG).show();
            }
            
            
            
            
        }
    }
    
    private String bytesToText(byte[] bytes) {
    	StringBuilder buffer = new StringBuilder();
    	for (byte b : bytes) {
    		String hex = String.format("%02X", b);
    		buffer.append(hex).append(" ");
    	}
    	
    	String text = buffer.toString().trim();
    	return text;
    }
    
    private boolean isTextRecord(NdefRecord record) {
    	  return record.getTnf() == NdefRecord.TNF_WELL_KNOWN
    	      && Arrays.equals(record.getType(), NdefRecord.RTD_TEXT);
    	}
    private String getTextAndLangCode(NdefRecord record) {
    	  if (record == null)
    	    throw new IllegalArgumentException();
    	 
    	  byte[] payload = record.getPayload();
    	  byte flags = payload[0];
    	  String encoding = ((flags & 0x80) == 0) ? "UTF-8" : "UTF-16";
    	  int languageCodeLength = flags & 0x3F;
    	  try {
    	    String languageCode = new String(payload, 1, languageCodeLength, "US-ASCII");
    	    String text = new String(payload, 1 + languageCodeLength, payload.length
    	        - (1 + languageCodeLength), encoding);
    	 
    	    return String.format("%s(%s)", text, languageCode);
    	  } catch (UnsupportedEncodingException e) {
    	    throw new IllegalArgumentException();
    	  } catch (IndexOutOfBoundsException e) {
    	    throw new IllegalArgumentException();
    	  }
    	}
    
    class HttpGetTask extends AsyncTask<HttpUriRequest, Void, HttpResponse> {
	    // doInBackground() に、バックグラウンド処理の内容を記述する。
	    protected HttpResponse doInBackground(HttpUriRequest... request) {
	        AndroidHttpClient httpClient = AndroidHttpClient.newInstance("Demo AndroidHttpClient");
	        HttpResponse response = null;
	    try {
	            response = httpClient.execute(request[0]);
	        } catch (IOException e) {
	            e.printStackTrace();
	        }
	        return response;
	    }

	    // onPostExecute() に、バックグラウンド処理完了時の処理を記述する。
	    protected void onPostExecute(HttpResponse response) {
	    	String json = "";
	    	String title = "ないよ";
	    	StringBuilder builder = new StringBuilder();
	    	
	    	//int statusCode = response.getStatusLine().getStatusCode();
	    	// ステータスコードを取得
	    	int statusCode = response.getStatusLine().getStatusCode();
	    	//レスポンスからHTTPエンティティ（実体）を生成
			HttpEntity entity = response.getEntity();
			//HTTPエンティティからコンテント（中身）を生成
			InputStream content = null;
			try {
				content = entity.getContent();
			} catch (IllegalStateException e1) {
				// TODO 自動生成された catch ブロック
				e1.printStackTrace();
			} catch (IOException e1) {
				// TODO 自動生成された catch ブロック
				e1.printStackTrace();
			}
			//コンテントからInputStreamReaderを生成し、さらにBufferedReaderを作る
			//InputStreamReaderはテキストファイル（InputStream）を読み込む
			//BufferedReaderはテキストファイルを一行ずつ読み込む
			//（参考）http://www.tohoho-web.com/java/file.htm
			BufferedReader reader = new BufferedReader(new InputStreamReader(content));
			String line = "";
			//readerからreadline()で行を読んで、builder文字列(StringBuilderクラス)に格納していく。
			//※このプログラムの場合、lineは一行でなのでループは回っていない
			//※BufferedReaderを使うときは一般にこのように記述する。
			try {
				while ((line = reader.readLine()) != null) {
					builder.append(line);
				}
			} catch (IOException e1) {
				// TODO 自動生成された catch ブロック
				e1.printStackTrace();
			}
		
	    	
	    	json = builder.toString();
	    	Log.d(TAG, json);
	    	JSONObject jsono = null;
			try {
				jsono = new JSONObject(json);
			} catch (JSONException e) {
				// TODO 自動生成された catch ブロック
				e.printStackTrace();
			}
			try {
				//title = jsono.getString("result");
				
				if(statusCode != 200){
					title = jsono.getString("header");
					jsono = new JSONObject(title);
					Log.d(TAG, jsono.getString("errorcode"));
					Log.d(TAG, jsono.getString("message"));
				}
				
				title = jsono.getString("access_token");
			} catch (JSONException e) {
				// TODO 自動生成された catch ブロック
				e.printStackTrace();
			}
	        
			
			
	    }
	}
	/*
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
	}*/
}
