package com.modifyhost;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class ListHostActivity extends Activity {

	private static final String HOST_FILE ="/etc/hosts";
	private ArrayList<Host> mHosts;
	private HostAdapeter mAdapter;
	private ListView mList;
	

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.list);
		mList = (ListView) findViewById(R.id.list);

		Button add = (Button) findViewById(R.id.add);
		add.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				showDialog(null);
			}
		});
		
		mList.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
				Host host = mAdapter.getItem(position);
				showDialog(host);
			}
		});
		
		mList.setOnItemLongClickListener(new OnItemLongClickListener() {

			@Override
			public boolean onItemLongClick(AdapterView<?> parent, View view,
					int position, long id) {
				final Host host = mAdapter.getItem(position);
				new AlertDialog.Builder(ListHostActivity.this)
                .setTitle("删除:" + host.ip)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                    	clearHostFile();
                    	mHosts.remove(host);
                    	for(Host h : mHosts) {
                    		String str = h.ip + " " + h.name;
                    		addHosts(str);
                    	}
                    	refresh();
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                    }
                })
                .create().show();
				return true;
			}
		});

	}
	
	@Override
	protected void onResume() {
		super.onResume();
		setSystemWritable();
		refresh();
	}
	
	private void refresh() {
		initData();
		mAdapter.notifyDataSetChanged();
	}

	private void setSystemWritable() {
		List<String> commands = new ArrayList<String>();
		commands.add("su");
		commands.add("|");
		commands.add("mount");
		commands.add("-o");
		commands.add("remount,rw");
		commands.add("-t");
		commands.add("yaffs2");
		commands.add("/dev/block/mtdblock3");
		commands.add("/system");
		runTime(commands);
	}
	
	private void addHosts(String str) {
		List<String> commands = new ArrayList<String>();
		commands.add("su");
		commands.add("|");
		commands.add("echo");
		commands.add(str);
		commands.add(">>");
		commands.add(HOST_FILE);
		runTime(commands);
	}
	
	private void clearHostFile() {
		List<String> commands = new ArrayList<String>();
		commands.add("su");
		commands.add("|");
		commands.add("cat");
		commands.add("/dev/null");
		commands.add(">");
		commands.add(HOST_FILE);
		runTime(commands);
	}
	
	private void runTime(List<String> commands) {
		ProcessBuilder pb = new ProcessBuilder(commands);
		try {
			Process p = pb.start();
			p.waitFor();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	private void showDialog(final Host host) {
		final View entryView = View.inflate(ListHostActivity.this, R.layout.modify, null);
		final EditText ip = (EditText) entryView.findViewById(R.id.ip);
		final EditText name = (EditText) entryView.findViewById(R.id.name);
		
		if(host != null) {
			ip.setText(host.ip);
			name.setText(host.name);
		}
		
		new AlertDialog.Builder(ListHostActivity.this)
				.setView(entryView)
				.setPositiveButton("OK", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {
						String strIp = ip.getText().toString().trim();
						String strName = name.getText().toString().trim();
						boolean pass = check(strIp, strName);
						if(pass) {
							if(host == null ||
									(host != null && !strIp.equals(host.ip))) {
								String lines = strIp + " " + strName;
								Log.i("peter", "lines = " + lines);
								addHosts(lines);
								refresh();
							}
							setColseDialog((AlertDialog)dialog, true);
						}else{
							setColseDialog((AlertDialog)dialog, false);
						}
					}
				})
				.setNegativeButton("Cancel",
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog,
									int whichButton) {
								setColseDialog((AlertDialog)dialog, true);
							}
						}).create().show();
	}
	
	private void setColseDialog(AlertDialog dialog, boolean close) {
		try {
			Field field = dialog.getClass().getSuperclass()
					.getDeclaredField("mShowing");
			field.setAccessible(true);
			field.set(dialog, close); // false
		} catch (SecurityException e) {
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (NoSuchFieldException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		}
	}
	
	private boolean check(String ip, String name) {
		
		boolean result = true;
		
		if(!TextUtils.isEmpty(ip)) {
			if(!isValidIP(ip)) {
				result = false;
				Toast.makeText(ListHostActivity.this, "ip 不合法", Toast.LENGTH_SHORT).show();
			}
		}else{
			result = false;
			Toast.makeText(ListHostActivity.this, "ip 不能为空", Toast.LENGTH_SHORT).show();
		}
		
		if(TextUtils.isEmpty(name)) {
			result = false;
			Toast.makeText(ListHostActivity.this, "name 不能为空", Toast.LENGTH_SHORT).show();
		}
		
		return result;
	}

	public class Host {
		String name;
		String ip;
	}

	private void initData() {
		mAdapter = new HostAdapeter();
		mHosts = new ArrayList<Host>();
		mList.setAdapter(mAdapter);
		File file = new File("/etc/hosts");
		BufferedReader reader = null;
		try {
			reader = new BufferedReader(new FileReader(file));
			String tempString = null;
			String ip = null;
			String name = null;
			while ((tempString = reader.readLine()) != null) {
				System.out.println("line " + ": " + tempString);
				String[] strs = tempString.split(" ");
				for (int i = 0; i < strs.length; i++) {
					String str = strs[i].trim();
					if (str.length() > 0) {
						if (isValidIP(str) || isLocalIp(str)) {
							ip = str;
						} else {
							name = str;
						}
					}
				}

				Host host = new Host();
				host.ip = ip;
				host.name = name;
				mHosts.add(host);
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally{
			if(reader != null) {
				try {
					reader.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}

	}

	private boolean isValidIP(String str) {
		String regex = "^(\\d{1,2}|1\\d\\d|2[0-4]\\d|25[0-5])\\."
				+ "(\\d{1,2}|1\\d\\d|2[0-4]\\d|25[0-5])\\."
				+ "(\\d{1,2}|1\\d\\d|2[0-4]\\d|25[0-5])\\."
				+ "(\\d{1,2}|1\\d\\d|2[0-4]\\d|25[0-5])$";
		Pattern pattern = Pattern.compile(regex);
		return pattern.matcher(str).matches();
	}

	private boolean isLocalIp(String str) {
		return "127.0.0.1".equals(str);
	}

	public class HostAdapeter extends BaseAdapter {

		@Override
		public int getCount() {
			return mHosts.size();
		}

		@Override
		public Host getItem(int position) {
			return mHosts.get(position);
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			if (convertView == null) {
				LayoutInflater factory = LayoutInflater
						.from(ListHostActivity.this);
				convertView = factory.inflate(R.layout.item, parent, false);
			}

			TextView ip = (TextView) convertView.findViewById(R.id.ip);
			TextView name = (TextView) convertView.findViewById(R.id.name);

			Host host = getItem(position);

			ip.setText(host.ip);
			name.setText(host.name);

			return convertView;
		}

	}

}
