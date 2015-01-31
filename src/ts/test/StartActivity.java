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
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
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
	private static final String HN_FILE = "HN.txt";
	private String HOSTNAME = "test";
	private TextView msg;
	private EditText id;
	private EditText pw;
	private EditText hn;
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
		hn = (EditText)findViewById(R.id.input_HN);

		Button start = (Button)findViewById(R.id.start);
		Button end = (Button)findViewById(R.id.end);
		Button write = (Button)findViewById(R.id.write);
		Button push = (Button)findViewById(R.id.push);
		Button test = (Button)findViewById(R.id.test);
		Button hnw = (Button)findViewById(R.id.HN);
		Button hnl = (Button)findViewById(R.id.HN_load);
		start.setOnClickListener(this);
		end.setOnClickListener(this);
		write.setOnClickListener(this);
		push.setOnClickListener(this);
		test.setOnClickListener(this);
		hnw.setOnClickListener(this);
		hnl.setOnClickListener(this);



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
		/*NdefMessage record = new NdefMessage(
                new NdefRecord[] { createMimeRecord(
                        "application/ts.test", url.getBytes())
                        ,NdefRecord.createApplicationRecord("ts.test")
        });
		Locale jp = new Locale("jp");
		NdefRecord record = createTextRecord(url, jp ,true );
		return new NdefMessage(new NdefRecord[]{record});
		*/
		String text = url;
        NdefRecord[] record = new NdefRecord[]{ createMimeRecord(
                "application/ts.test", text.getBytes())};
        NdefMessage msg = new NdefMessage(record);
        return msg;
	}

	//ダグ作成2
	public NdefRecord createMimeRecord(String mimeType, byte[] payload) {
		byte[] mimeBytes = mimeType.getBytes(Charset.forName("US-ASCII"));
		NdefRecord mimeRecord = new NdefRecord(
				NdefRecord.TNF_MIME_MEDIA, mimeBytes, new byte[0], payload);
		return mimeRecord;
	}


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


	private boolean writeNdefMessage(Ndef ndefTag, NdefMessage ndefMessage) {
		if (!ndefTag.isWritable()) { 
			// このタグは書き込めないので、何もしないでreturnする。
			Log.d(TAG, "このタグはRead Onlyです。");
			return false;
		}
		int messageSize = ndefMessage.toByteArray().length;
		if (messageSize > ndefTag.getMaxSize()) { 
			// タグの書き込み可能サイズを超えているので、書き込めない。
			return false;

		}

		try {
			if (!ndefTag.isConnected()) {
				ndefTag.connect(); 
			}
			ndefTag.writeNdefMessage(ndefMessage); 
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
				ndefTag.close(); 
			} catch (IOException e) {

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
		case R.id.push: //認証要求
			if(!hn.getText().toString().equals("")){
				HOSTNAME = hn.getText().toString();
			}
			String buf = "";
			buf = id.getText().toString() + ":" + pw.getText().toString();
			Log.d(TAG, buf);
			String encode = Base64.encodeToString(buf.getBytes(), Base64.DEFAULT);
			Log.d(TAG, "encode:" + encode);

			//HttpGet request = new HttpGet("http://www.finds.jp/ws/rgeocode.php?lat=35.6853264&lon=139.7530997&json");
			HttpPost request = new HttpPost(HOSTNAME + "/api/1.0/auth/access_token");
			//HttpPost request = new HttpPost(HOSTNAME + "/auth/access_token");
			//HttpPost request = new HttpPost("https://www.google.co.jp");
			Log.d(TAG, HOSTNAME + "/api/1.0/auth/access_token");
			request.setHeader("Authorization", "Basic " + encode);
			request.setHeader("content-type", "application/json");
			request.setHeader("Accept", "application/json");
			


			new HttpGetTask().execute(request);

			break;

		case R.id.test: //httpリクエストのテスト
			//String buf2 = "";
			//buf2 = id.getText().toString() + ":" + pw.getText().toString();
			//Toast.makeText(this, buf2, Toast.LENGTH_LONG).show();

			HttpPost request2 = new HttpPost("http://www.finds.jp/ws/rgeocode.php?lat=35.6853264&lon=139.7530997&json");
			request2.setHeader("Authorization", "Basic " + "");
			

			new HttpGetTask().execute(request2);

			break;

		case R.id.HN: //ホストネームの書き込み
			OutputStream out2;
			try {
				out2 = openFileOutput(HN_FILE,MODE_PRIVATE);
				PrintWriter writer = new PrintWriter(new OutputStreamWriter(out2,"UTF-8"));

				//追記する
				writer.append(hn.getText().toString());
				writer.close();
				Toast.makeText(this, "ホスト名を記録しました", Toast.LENGTH_SHORT).show();
			} catch (IOException e) {
				// TODO 自動生成された catch ブロック
				e.printStackTrace();
			}

			break;
			
		case R.id.HN_load: //ホストネームの書き込み
		      //HNファイルの読み込み
	        InputStream in2;
	        String lineBuffer2 ="";
	        try {
	            in2 = openFileInput(HN_FILE); 
	     
	            BufferedReader reader= new BufferedReader(new InputStreamReader(in2,"UTF-8"));
	            while( (lineBuffer2 = reader.readLine()) != null ){
	                Log.d("HN",lineBuffer2);
	                if(!lineBuffer2.equals("")){
	                	HOSTNAME = lineBuffer2; 
	                	hn.setText(HOSTNAME);

	                }
	            }
	        } catch (IOException e) {
	            // TODO 自動生成された catch ブロック
	            e.printStackTrace();
	        }

			break;
		}

	}


	class HttpGetTask extends AsyncTask<HttpUriRequest, Void, HttpResponse> {
		// doInBackground() に、バックグラウンド処理の内容を記述する。
		protected HttpResponse doInBackground(HttpUriRequest... request) {

			AndroidHttpClient httpClient = AndroidHttpClient.newInstance("Demo AndroidHttpClient");
			HttpParams params = httpClient.getParams();
			Log.d(TAG, "http.protocol.version:" + params.getParameter("http.protocol.version"));
			Log.d(TAG, "http.protocol.content-charset:" + params.getParameter("http.protocol.content-charset"));
			Log.d(TAG, "http.protocol.handle-redirects:" + params.getParameter("http.protocol.handle-redirects"));
			Log.d(TAG, "http.conn-manager.timeout:" + params.getParameter("http.conn-manager.timeout"));
			Log.d(TAG, "http.socket.timeout:" + params.getParameter("http.socket.timeout"));
			Log.d(TAG, "http.connection.timeout:" + params.getParameter("http.connection.timeout"));
			
			HttpResponse response = null;

			try {
				response = httpClient.execute(request[0]);
			} catch (IOException e) {
				e.printStackTrace();
			}
			//httpClient.close();
			return response;
		}

		// onPostExecute() に、バックグラウンド処理完了時の処理を記述する。
		protected void onPostExecute(HttpResponse response) {
			String json = "";
			String title = "ないよ";
			StringBuilder builder = new StringBuilder();
			Log.d(TAG, "かえってきた");
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
					Log.d(TAG, line);
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
			Log.d(TAG,  String.valueOf(response.getStatusLine().getStatusCode()));
			if(response.getStatusLine().getStatusCode() == 200){
				try {
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
				Toast.makeText(getApplicationContext(), "認証完了", Toast.LENGTH_SHORT).show();
			}else{
				Toast.makeText(getApplicationContext(), "認証に失敗しました", Toast.LENGTH_SHORT).show();
			}
		}
	}



}



