package com.librelio.view;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.librelio.LibrelioApplication;
import com.librelio.model.dictitem.MagazineItem;
import com.librelio.storage.MagazineManager;
import com.niveales.wind.R;
import com.squareup.picasso.Picasso;

public class DownloadedMagazinesListView extends ListView {

	private Context context;
	private MagazinesAdapter magazinesAdapter;

	public DownloadedMagazinesListView(Context context, AttributeSet attrs,
			int defStyle) {
		super(context, attrs, defStyle);
		this.context = context;
		setOnItemClickListener();

		magazinesAdapter = new MagazinesAdapter(context);
		setAdapter(magazinesAdapter);
	}

	private void setOnItemClickListener() {
		setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {

				MagazineItem downloadedMagazine = magazinesAdapter
						.getItem(position);
				LibrelioApplication.startPDFActivity(
						context,
						downloadedMagazine.isSample() ? downloadedMagazine
								.getSamplePdfPath() : downloadedMagazine
								.getItemFileName(), downloadedMagazine
								.getTitle(), true);
			}
		});
	}

	public DownloadedMagazinesListView(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public DownloadedMagazinesListView(Context context) {
		this(context, null, 0);
	}

	public void setMagazines(Activity activity, List<MagazineItem> downloads) {
		magazinesAdapter.setDownloads(activity, downloads);
	}
}

class MagazinesAdapter extends ArrayAdapter<MagazineItem> {

	private Context context;
	private List<MagazineItem> downloads;
	private String samplePostfix;

	public MagazinesAdapter(Context context) {
		super(context, R.layout.row_downloaded_magazines, 0);
		this.context = context;
		downloads = new ArrayList<MagazineItem>();

		samplePostfix = new StringBuilder(" (")
				.append(context.getString(R.string.sample)).append(")")
				.toString();
	}

	public void setDownloads(Activity activity,
			final List<MagazineItem> newDownloads) {
		activity.runOnUiThread(new Runnable() {
			@Override
			public void run() {
				downloads.clear();
				downloads.addAll(newDownloads);
				notifyDataSetChanged();
			}
		});
	}

	@Override
	public int getCount() {
		return downloads.size();
	}

	@Override
	public MagazineItem getItem(int position) {
		return downloads.get(position);
	}

	static class ViewHolder {
		public ImageView image;
		public TextView title;
		public TextView editionDate;
		public TextView downloadDate;
		public Button deleteButton;
		public int position = -1;
	}

	@Override
	public View getView(final int position, View convertView, ViewGroup parent) {

		final ViewHolder holder;
		if (position < this.downloads.size()) {
			final MagazineItem downloadedMagazine = this.downloads.get(position);

			if ((convertView == null) || (null == convertView.getTag())) {

				holder = new ViewHolder();

				LayoutInflater inflater = LayoutInflater.from(context);
				convertView = inflater.inflate(
						R.layout.row_downloaded_magazines, null);

				ImageView image = (ImageView) convertView
						.findViewById(R.id.downloaded_magazines_item_image);
				holder.image = image;

				TextView title = (TextView) convertView
						.findViewById(R.id.downloaded_magazines_item_title);
				holder.title = title;

				TextView editionDate = (TextView) convertView
						.findViewById(R.id.downloaded_magazines_item_edition_date);
				holder.editionDate = editionDate;

				TextView downloadDate = (TextView) convertView
						.findViewById(R.id.downloaded_magazines_item_download_date);
				holder.downloadDate = downloadDate;

				Button deleteButton = (Button) convertView
						.findViewById(R.id.downloaded_magazines_item_delete_button);
				holder.deleteButton = deleteButton;
				holder.deleteButton.setFocusable(false);
				holder.deleteButton.setFocusableInTouchMode(false);

				convertView.setTag(holder);
			} else {
				holder = (ViewHolder) convertView.getTag();
			}

			holder.title.setText(downloadedMagazine.getTitle()
					+ (downloadedMagazine.isSample() ? samplePostfix : ""));
			holder.editionDate.setText(downloadedMagazine.getSubtitle());
			holder.downloadDate.setText(getContext().getString(R.string.downloaded_title) + downloadedMagazine.getDownloadDate());

			holder.deleteButton.setText(getContext().getString(R.string.delete));
			holder.deleteButton.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View v) {
					holder.deleteButton.setText("...");
					new Thread(new Runnable() {
						@Override
						public void run() {
							downloadedMagazine.deleteMagazine();
						}
					}).start();
				}
			});
			holder.position = position;

			Picasso.with(context).load(downloadedMagazine.getPngUri()).fit()
					.centerInside().into(holder.image);
		} else {
			LayoutInflater inflater = LayoutInflater.from(context);
			convertView = inflater.inflate(R.layout.row_downloaded_magazines,
					null);
		}

		return convertView;
	}
}
