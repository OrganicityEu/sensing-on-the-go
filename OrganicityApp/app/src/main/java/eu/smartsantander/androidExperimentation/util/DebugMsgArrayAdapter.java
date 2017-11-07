//package eu.smartsantander.androidExperimentation.util;
//
//import android.app.Activity;
//import android.content.Context;
//import android.util.Log;
//import android.view.LayoutInflater;
//import android.view.View;
//import android.view.ViewGroup;
//import android.widget.ArrayAdapter;
//import android.widget.TextView;
//
//import java.util.ArrayList;
//
//import eu.organicity.set.app.R;
//
//public class DebugMsgArrayAdapter extends ArrayAdapter<DebugMsg> {
//
//	private final ArrayList<DebugMsg> messages;
//    private final Activity activity;
//    private final String TAG = "DBG ADAPTER";
//
//	public DebugMsgArrayAdapter(Activity act, int resource, ArrayList<DebugMsg> objects) {
//
//		super(act, resource, objects);
//		activity = act;
//		messages = objects;
//	}
//
//	@Override
//    public View getView(int position, View convertView, ViewGroup parent) {
//
//		View view = convertView;
//
//        if (view == null) {
//            LayoutInflater vi = (LayoutInflater)activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
//            view = vi.inflate(R.layout.list_item_dbg, null);
//
//        }
//
//        if ( messages.get(position) != null ) {
//
//        	TextView tv1 = (TextView)view.findViewById(R.id.debug_msg_timestamp);
//        	TextView tv2 = (TextView)view.findViewById(R.id.debug_msg_content);
//
//        	tv1.setText(messages.get(position).getDate().toLocaleString());
//        	tv2.setText(messages.get(position).getMsg());
//
//        	Log.w(TAG, "TIMESTAMP IS " + tv1.getText());
//        }
//
//		return view;
//
//	}
//
//
//}
