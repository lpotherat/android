package com.librelio;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;

import javax.net.ssl.SSLContext;

import org.acra.ACRA;
import org.acra.ReportField;
import org.acra.ReportingInteractionMode;
import org.acra.annotation.ReportsCrashes;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.res.AssetManager;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Environment;
import android.provider.Settings;
import android.util.Log;

import com.librelio.activity.MuPDFActivity;
import com.librelio.library.ui.NivealesApplication;
import com.librelio.library.ui.criteraselectors.CheckedCriteriaSelectorFragment;
import com.librelio.library.ui.criteraselectors.CheckedCriteriaSelectorFragment.OnCriteriaChangedListener;
import com.librelio.library.ui.criteraselectors.RangeCriteriaSelectorFragment;
import com.librelio.library.ui.criteraselectors.RangeCriteriaSelectorFragment.OnRangeCriteriaChangedListener;
import com.librelio.library.ui.lexique.LexiqueFragment;
import com.librelio.library.ui.productdetail.ProductDetailFragment;
import com.librelio.library.ui.productdetail.ProductDetailFragment.ShareProductListener;
import com.librelio.library.ui.productlist.FavoriteProductListFragment;
import com.librelio.library.ui.productlist.ProductListFragment;
import com.librelio.library.ui.productsearch.ProductSearchFragment;
import com.librelio.library.utils.adapters.CursorViewBinder;
import com.librelio.library.utils.db.DBHelper;
import com.librelio.storage.DataBaseHelper;
import com.librelio.utils.GooglePlayServicesUtils;
import com.librelio.utils.SystemHelper;
import com.niveales.wind.BuildConfig;
import com.niveales.wind.R;
import com.squareup.okhttp.OkHttpClient;

@ReportsCrashes(formKey = "",
        mailTo = "android@librelio.com",
        customReportContent = { ReportField.APP_VERSION_CODE, ReportField.APP_VERSION_NAME,
                ReportField.ANDROID_VERSION,
                ReportField.PHONE_MODEL, ReportField.CUSTOM_DATA, ReportField.STACK_TRACE, ReportField.LOGCAT },
        mode = ReportingInteractionMode.TOAST,
        resToastText = R.string.crash_toast_text)
public class LibrelioApplication extends Application {
	public static final String SUBSCRIPTION_YEAR_KEY = "yearlysubscription";
	public static final String SUBSCRIPTION_MONTHLY_KEY = "monthlysubscription";

	private static final String TAG = "LibrelioApplication";
	private static final String PATH_SEPARATOR = "/";
	
//	private static final String SERVER_URL = "http://php.netcook.org/librelio-server/downloads/android_verify.php";
	
	private static String baseUrl;
	private static OkHttpClient client;

	@Override
	public void onCreate() {
        super.onCreate();
        ACRA.init(this);

        baseUrl = "http://librelio-europe.s3.amazonaws.com/" + getClientName(this) + PATH_SEPARATOR + getMagazineName(this) + PATH_SEPARATOR;


//		baseUrl = "http://librelio-test.s3.amazonaws.com/" + getMagazineName(this) +
//                PATH_SEPARATOR;

        registerForGCM();
    }

	public static OkHttpClient getOkHttpClient() {
		if (client == null) {
			client = new OkHttpClient();
			// Fix for https://github.com/square/okhttp/issues/184
			// Although it should be fixed in OkHttp 2
			SSLContext sslContext;
			try {
				sslContext = SSLContext.getInstance("TLS");
				sslContext.init(null, null, null);
			} catch (GeneralSecurityException e) {
				throw new AssertionError(); // The system has no TLS. Just give
											// up.
			}
			client.setSslSocketFactory(sslContext.getSocketFactory());
		}
		return client;
	}

	private void registerForGCM() {
        // Check device for Play Services APK. If check succeeds, proceed with
        //  GCM registration.
        if (GooglePlayServicesUtils.checkPlayServices(getApplicationContext())) {
            String regid = GooglePlayServicesUtils.getRegistrationId(getApplicationContext());
            
            if (BuildConfig.DEBUG) {
            	Log.i(TAG, "current GCM RegistrationID = " + regid);
            }

            // if regid not stored in SharedPreferences then register for GCM
            if (regid.isEmpty()) {
                GooglePlayServicesUtils.registerInBackground(getApplicationContext());
            }
        } else {
            Log.i(TAG, "No valid Google Play Services APK found.");
        }
	}


	public static void startPDFActivity(Context context, String filePath, String title, boolean showThumbnails){
		try{
			Uri uri = Uri.parse(filePath);
			Intent intent = new Intent(context,MuPDFActivity.class);
			intent.setAction(Intent.ACTION_VIEW);
			intent.setData(uri);
			intent.putExtra(DataBaseHelper.FIELD_TITLE, title);
			intent.putExtra(MuPDFActivity.SHOW_THUMBNAILS_EXTRA, showThumbnails);
			context.startActivity(intent);
		} catch (Exception e) {
			Log.e(TAG,"Problem with starting PDF-activity, path: "+filePath,e);
		}

	}

	public static boolean thereIsConnection(Context context) {
		
		if (SystemHelper.isEmulator(context)) {
			return true;
		}
		
		ConnectivityManager conMgr = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo i = conMgr.getActiveNetworkInfo();
		if (i == null) {
			return false;
		}
		if (!i.isConnected()) {
			return false;
		}
		if (!i.isAvailable()) {
			return false;
		}
		return true;
	}

	public static String getClientName(Context context){
		String packageName = context.getPackageName();
		int lastIndexOfDot = packageName.lastIndexOf(".");
		return packageName.substring(packageName.lastIndexOf(".", lastIndexOfDot - 1) + 1, lastIndexOfDot);
	}
	
	public static String getMagazineName(Context context){
		String packageName = context.getPackageName();
		return packageName.substring(packageName.lastIndexOf(".") + 1);
	}

    public static String getServiceName(Context context){
        return context.getResources().getString(R.string.user_service);
    }
	
	public static String getUrlString(String fileName){
		return PATH_SEPARATOR + fileName;
	}
	
	public static String getUrlString(Context context, String fileName){
		return LibrelioApplication.getClientName(context) + PATH_SEPARATOR 
		+ LibrelioApplication.getMagazineName(context) + PATH_SEPARATOR + fileName;
	}

	public static String getYearlySubsCode(Context context){
		return context.getResources().getString(R.string.yearly_subs_code);
	}

	public static String getMonthlySubsCode(Context context){
		return context.getResources().getString(R.string.monthly_subs_code);
	}
	
	public static boolean isEnableCodeSubs(Context context){
		return !context.getResources().getString(R.string.code_service).isEmpty();
	}

    public static boolean isEnableUsernamePasswordLogin(Context context){
        return !context.getResources().getString(R.string.user_service).isEmpty();
    }

	public static String getServerUrl(Context context){
		return context.getString(R.string.server_url);
	}

	public static String getAmazonServerUrl(){
		return baseUrl;
	}

    public static String getAndroidId(Context context) {
        return Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
    }
}
