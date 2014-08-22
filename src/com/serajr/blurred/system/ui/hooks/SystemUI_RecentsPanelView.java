package com.serajr.blurred.system.ui.hooks;

import android.content.res.Resources;
import android.content.res.Resources.NotFoundException;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.view.View;
import android.view.ViewGroup;

import com.android.systemui.recent.RecentsPanelView;
import com.serajr.blurred.system.ui.R;
import com.serajr.blurred.system.ui.Xposed;
import com.serajr.blurred.system.ui.fragments.BlurSettings_Fragment;
import com.serajr.utils.BlurUtils;
import com.serajr.utils.Utils;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

public class SystemUI_RecentsPanelView {
	
	private static Bitmap mBlurredBitmap;
	private static boolean mBlurredRecentAppsEnabled;
	private static RecentsPanelView mRecentsPanelView;
	
	public static void hook() {
		
		try {
			
			// constructor
			XposedBridge.hookAllConstructors(RecentsPanelView.class, new XC_MethodHook() {
				
				@Override
	            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
					
					// guarda
					mRecentsPanelView = (RecentsPanelView) param.thisObject;
					
				}
			});
			
			// dispatchDraw
			XposedHelpers.findAndHookMethod(
					Utils.getAndroidAPILevel() >= 19
						// >= 4.4
						? RecentsPanelView.class
						// <= 4.3
						: ViewGroup.class,
					"dispatchDraw", Canvas.class, new XC_MethodHook() {
				
				@Override
	            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
					
					// ------------------
					// beforeHookedMethod
					// ------------------
					
					// objeto � o mRecentsPanelView ?
					if (param.thisObject == mRecentsPanelView) {
					
						// ----------------------------------------------------
						// necess�rio essa verifica��o, pois android <= 4.3 n�o
						// tem o m�todo dispatchDraw dentro do RecentsPanelView
						// ent�o o hook acontece dentro do ViewGroup, que passa
						// por todo o sistema. � mRecentsPanelView ? continua !  
						// ----------------------------------------------------
						
						// continua ?
						if (mBlurredBitmap != null) {
							
							Canvas canvas = (Canvas) param.args[0]; 
							
							// limites
							Rect src = new Rect(0, SystemUI_PhoneStatusBar.mStatusBarHeight, canvas.getWidth(), SystemUI_PhoneStatusBar.mStatusBarHeight + canvas.getHeight());
							Rect dst = new Rect(0, 0, canvas.getWidth(), canvas.getHeight());
							
							// desenha
							((Canvas) param.args[0]).drawBitmap(mBlurredBitmap, src, dst, null);
							
						}
					}
				}
			});
			
			// showIfReady
			XposedHelpers.findAndHookMethod(RecentsPanelView.class, "showIfReady", new XC_MethodHook() {
				
				@Override
	            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
					
					// continua ?
					if (!mBlurredRecentAppsEnabled)
						return;
					
					// obt�m o recents_bg_protect
					View view = null;
					int viewResId = mRecentsPanelView.getResources().getIdentifier("recents_bg_protect", "id", Xposed.SYSTEM_UI_PACKAGE_NAME);
					if (viewResId != 0)						
						view = mRecentsPanelView.findViewById(viewResId);
					
					// seta o fundo padr�o transparente para o recents_bg_protect
					if (view != null)
						view.setBackground(new ColorDrawable(Color.TRANSPARENT));
					
				}
			});
			
			// dismiss
			XposedHelpers.findAndHookMethod(RecentsPanelView.class, "dismiss", new XC_MethodHook() {
				
				@Override
	            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
					
					// padr�es
					defaults();
					
				}
			});
			
			// >= 4.2 (<= 4.1 n�o tem esse m�todo)
			if (Utils.getAndroidAPILevel() >= 17) {
				
				// dismissAndGoBack
				XposedHelpers.findAndHookMethod(RecentsPanelView.class, "dismissAndGoBack", new XC_MethodHook() {
					
					@Override
		            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
						
						// padr�es
						defaults();
						
					}
				});
			}
							
		} catch (Exception e) {
			
			XposedBridge.log(e);
			
		}
	}
	
	public static void updatePreferences(XSharedPreferences prefs) {
		
		// atualiza
		mBlurredRecentAppsEnabled = prefs.getBoolean(BlurSettings_Fragment.RECENT_APPS_ENABLED_PREFERENCE_KEY, BlurSettings_Fragment.RECENT_APPS_ENABLED_PREFERENCE_DEFAULT);
		
		// padr�es
		defaults();
		
	}
	
	public static void onConfigurationChanged() {
		
		// -----------------
		// alterou a rota��o
		// -----------------
		
		// padr�es
		defaults();
		
	}
	
	public static void blur() {
		
		// continua ?
		if (!mBlurredRecentAppsEnabled)
			return;
		
		// blur (true - retorna o bitmap no tamanho da tela !!!)
		BlurUtils.BlurTask.setOnBlurTaskCallback(new BlurUtils.BlurTask.BlurTaskCallback() {
			
			@Override
			public void blurTaskDone(Bitmap blurredBitmap) {
				
				// guarda
				mBlurredBitmap = blurredBitmap;
				
				// redesenha ?
				if (mRecentsPanelView != null)
					mRecentsPanelView.postInvalidate();
				
			}
		}, true);
		new BlurUtils.BlurTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
		
	}
	
	private static void defaults() {
		
		// continua ?
		if (mRecentsPanelView == null)
			return;
		
		// ------------------------
		// seta o background padr�o
		// ------------------------
		
		Resources res = mRecentsPanelView.getResources();
		
		// obt�m o recents_bg_protect
		View view = null;
		int viewResId = res.getIdentifier("recents_bg_protect", "id", Xposed.SYSTEM_UI_PACKAGE_NAME);
		if (viewResId != 0)
			view = mRecentsPanelView.findViewById(viewResId);
		
		// obt�m o fundo padr�o
		Drawable viewBg = null;
		int viewBgResId = res.getIdentifier("status_bar_recents_background", "drawable", Xposed.SYSTEM_UI_PACKAGE_NAME);
		if (viewBgResId != 0) {
			
			try {
				
				viewBg = res.getDrawable(viewBgResId);
				
			} catch (NotFoundException e) {
				
				// --------------------
				// erro n�o esperado !!
				// --------------------
				
				e.printStackTrace();
				
			}
		}
		
		// n�o encontrou o drawable por
		// algum motivo inesperado !!!!
		if (viewBg == null) {
			
			// carrega o drawable do m�dulo
			viewBg = Xposed.getXposedModuleResources().getDrawable(R.drawable.status_bar_recents_background);
			
		}
		
		// seta o fundo padr�o para o recents_bg_protect
		if (view != null && viewBg != null)
			view.setBackground(viewBg);
		
		// recicla e anula ?
		if (mBlurredBitmap != null) {
			
			mBlurredBitmap.recycle();
			mBlurredBitmap = null;
			
		}
	}
}