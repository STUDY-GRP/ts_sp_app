package ts.test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.util.Locale;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Intent;
import android.net.http.AndroidHttpClient;
import android.nfc.FormatException;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.TagLostException;
import android.nfc.tech.Ndef;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class StartActivity extends Activity implements OnClickListener{
	NfcAdapter mNfcAdapter;
	private static final String TAG = "StartActivity";
	private String COMMAND = "";
	private static final String S_MSG = "出勤ダグを作成します";
	private static final String E_MSG = "退勤ダグを作成します";
	private static final String LOCAL_FILE = "key.txt";
	
	private TextView msg;
	private EditText id;
	private EditText pw;
	@Override
	public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //NFCを扱うためのインスタンスを取得
        mNfcAdapter = NfcAdapter.getDefaultAdapter(this);
        
        setContentView(R.layout.activity_start);
        msg = (TextView)findViewById(R.id.msg);
        COMMAND = "start";
        msg.setText(S_MSG);
        
        id = (EditText)findViewById(R.id.input_id);
        pw = (EditText)findViewById(R.id.input_pw);
    
        Button start = (Button)findViewById(R.id.start);
        Button end = (Button)findViewById(R.id.end);
        Button write = (Button)findViewById(R.id.write);
        Button push = (Button)findViewById(R.id.push);
        Button test = (Button)findViewById(R.id.test);
        start.setOnClickListener(this);
        end.setOnClickListener(this);
        write.setOnClickListener(this);
        push.setOnClickListener(this);
        test.setOnClickListener(this);
        
        
        
	}

    //タグが検知された 時のイベント
	protected void onNewIntent(Intent intent) {
		super.onNewIntent(intent);
        Tag tag = (Tag) intent.getParcelableExtra(NfcAdapter.EXTRA_TAG); //【1】
        if (tag == null) {
            // NFCタグがかざされたのとは異なる理由でインテントが飛んできた
            Log.d(TAG, "想定外のIntent受信です:");
            return;
        }
        
        Ndef ndefTag = Ndef.get(tag); // 【2】
        if (ndefTag == null) {
            // NDEFフォーマットされていないタグがかざされた
            Log.d(TAG, "NDEF形式ではないタグがかざされました。");
            return;
        }
        
        NdefMessage ndefMessage = createSmartPoster(COMMAND);
        if (writeNdefMessage(ndefTag, ndefMessage)) {
            Toast.makeText(this, "書き込みに成功しました。", Toast.LENGTH_LONG).show();
            Log.d(TAG, "書き込みが成功しました");
        } else {
            Toast.makeText(this, "書き込みに失敗しました。", Toast.LENGTH_LONG).show();
            Log.d(TAG, "書き込みが失敗しました");
        }
	}
	
	//ダグ作成1
	public NdefMessage createSmartPoster(String url) {
		/*NdefMessage spPayload = new NdefMessage(
                new NdefRecord[] { createMimeRecord(
                        "application/ts.test", url.getBytes())
                        ,NdefRecord.createApplicationRecord("ts.test")
        });*/
		Locale jp = new Locale("jp");
		NdefRecord record = createTextRecord(url, jp ,true );
	    return new NdefMessage(new NdefRecord[]{record});
	}
	
	//ダグ作成2
	public NdefRecord createMimeRecord(String mimeType, byte[] payload) {
        byte[] mimeBytes = mimeType.getBytes(Charset.forName("US-ASCII"));
        NdefRecord mimeRecord = new NdefRecord(
                NdefRecord.TNF_MIME_MEDIA, mimeBytes, new byte[0], payload);
        return mimeRecord;
    }
	
	/*
	//ダグ作成3
	private NdefRecord createUriRecord(String url) {
	    return NdefRecord.createUri(url);
	}
	
	//ダグ作成4
	private NdefRecord createActionRecord() {
	    byte[] typeField = "act".getBytes(Charset.forName("US-ASCII"));
	    byte[] payload = {(byte) 0x00};
	    return new NdefRecord(NdefRecord.TNF_WELL_KNOWN, 
	                          typeField,
	                          new byte[0],
	                          payload); // 【2】
	}
	*/
	
	@Override
    public void onResume() {
        super.onResume();
        PendingIntent pi = createPendingIntent(); 
        mNfcAdapter.enableForegroundDispatch(this, pi, null, null);
    }
	
	@Override
    protected void onPause() {
        super.onPause();
        mNfcAdapter.disableForegroundDispatch(this); // 【4】
    }
	
	private PendingIntent createPendingIntent() {
        Intent i = new Intent(this, StartActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP
                | Intent.FLAG_ACTIVITY_NEW_TASK); // 【5】
        return PendingIntent.getActivity(this, 0, i, 0);
    }

	
	
	
    /*public NdefRecord createMimeRecord(String mimeType, byte[] payload) {
        byte[] mimeBytes = mimeType.getBytes(Charset.forName("US-ASCII"));
        NdefRecord mimeRecord = new NdefRecord(
                NdefRecord.TNF_MIME_MEDIA, mimeBytes, new byte[0], payload);
        return mimeRecord;
    }*/
    
    private boolean writeNdefMessage(Ndef ndefTag, NdefMessage ndefMessage) {
        if (!ndefTag.isWritable()) {   // 【1】
            // このタグは書き込めないので、何もしないでreturnする。
            Log.d(TAG, "このタグはRead Onlyです。");
            return false;
        }
        int messageSize = ndefMessage.toByteArray().length;
        if (messageSize > ndefTag.getMaxSize()) { // 【2】
            // タグの書き込み可能サイズを超えているので、書き込めない。
            return false;

        }

        try {
            if (!ndefTag.isConnected()) {
                ndefTag.connect();  // 【3】
            }
            ndefTag.writeNdefMessage(ndefMessage); // 【4】
            return true;
        } catch (TagLostException e) {
            // Tagが途中で離された。
            Log.d(TAG, "タグが途中で離れてしまいました。", e);
            return false;
        } catch (IOException e) {
            // その他のIOエラー
            Log.d(TAG, "IOエラーです。", e);
            return false;
        } catch (FormatException e) {
            // 書き込もおうとしているNDEFメッセージが壊れている。
            Log.d(TAG, "書き込もうとしているNDEFメッセージが壊れています", e);
            return false;
        } finally {
            try {
                ndefTag.close(); // 【5】
            } catch (IOException e) {
                // ignore.
            }
        }
    }


	
	public NdefRecord createTextRecord(String payload, Locale locale, boolean encodeInUtf8) {
	    byte[] langBytes = locale.getLanguage().getBytes(Charset.forName("US-ASCII"));
	    Charset utfEncoding = encodeInUtf8 ? Charset.forName("UTF-8") : Charset.forName("UTF-16");
	    byte[] textBytes = payload.getBytes(utfEncoding);
	    int utfBit = encodeInUtf8 ? 0 : (1 << 7);
	    char status = (char) (utfBit + langBytes.length);
	    byte[] data = new byte[1 + langBytes.length + textBytes.length];
	    data[0] = (byte) status;
	    System.arraycopy(langBytes, 0, data, 1, langBytes.length);
	    System.arraycopy(textBytes, 0, data, 1 + langBytes.length, textBytes.length);
	    NdefRecord record = new NdefRecord(NdefRecord.TNF_WELL_KNOWN,
	    NdefRecord.RTD_TEXT, new byte[0], data);
	    return record;
	}

	/**
	 * ボタンのイベント制御
	 */
	@Override
	public void onClick(View v) {
		switch(v.getId()){
		
		case R.id.start: //出勤タグ
			COMMAND = "start";
			msg.setText(S_MSG);
			break;
			 
			
		case R.id.end: //退勤タグ
			COMMAND = "end";
			msg.setText(E_MSG);
			break;
				
		case R.id.write: //ファイルへの書き込みテスト
			OutputStream out;
		    try {
		        out = openFileOutput(LOCAL_FILE,MODE_PRIVATE);
		        PrintWriter writer = new PrintWriter(new OutputStreamWriter(out,"UTF-8"));
		 
		        //追記する
		        writer.append("0123456789");
		        writer.close();
		    } catch (IOException e) {
		        // TODO 自動生成された catch ブロック
		        e.printStackTrace();
		    }
			
			break;
		case R.id.push: //httpリクエストのテスト
			String buf = "";
			buf = id.getText().toString() + ":" + pw.getText().toString();
			String encode = Base64.encodeToString(buf.getBytes(), Base64.DEFAULT);
			Toast.makeText(this, encode, Toast.LENGTH_LONG).show();
			
			//HttpGet request = new HttpGet("http://www.finds.jp/ws/rgeocode.php?lat=35.6853264&lon=139.7530997&json");
			HttpPost request = new HttpPost("https://ec2-54-65-77-17.ap-northeast-1.compute.amazonaws.com:3000/api/1.0/auth/access_token");
			request.setHeader("Authorization", "Basic " + encode);
			
			new HttpGetTask().execute(request);

			break;
		
		case R.id.test: //httpリクエストのテスト
			String buf2 = "";
			buf2 = id.getText().toString() + ":" + pw.getText().toString();
			Toast.makeText(this, buf2, Toast.LENGTH_LONG).show();
			
			HttpGet request2 = new HttpGet("http://www.finds.jp/ws/rgeocode.php?lat=35.6853264&lon=139.7530997&json");
		    new HttpGetTask().execute(request2);

			break;
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
				title = jsono.getString("body");
				jsono = new JSONObject(title);
				title = jsono.getString("access_token");
			} catch (JSONException e) {
				// TODO 自動生成された catch ブロック
				e.printStackTrace();
			}
	        
			//ファイルへの出力
			OutputStream out;
		    try {
		        out = openFileOutput(LOCAL_FILE,MODE_PRIVATE);
		        PrintWriter writer = new PrintWriter(new OutputStreamWriter(out,"UTF-8"));
		 
		        //追記する
		        writer.append(title);
		        writer.close();
		    } catch (IOException e) {
		        // TODO 自動生成された catch ブロック
		        e.printStackTrace();
		    }
			
	    }
	}


	
}



