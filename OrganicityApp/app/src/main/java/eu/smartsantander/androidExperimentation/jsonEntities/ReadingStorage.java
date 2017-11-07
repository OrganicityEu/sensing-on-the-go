package eu.smartsantander.androidExperimentation.jsonEntities;

import android.os.Bundle;

import java.util.HashMap;

import eu.organicity.set.app.sdk.Reading;

public class ReadingStorage {

	final HashMap<String, Reading> store;

	public ReadingStorage() {
		this.store = new HashMap<>();
	}

	public synchronized void pushReading(Reading r) {
		this.store.put(r.getContext(), r);
	}

	public synchronized Reading getReading(String context) {
		return this.store.get(context);
	}

	public synchronized Bundle getBundle() {
		Bundle b = new Bundle();
		for (String context : store.keySet()) {
			Reading r = store.get(context);
			b.putString(context, r.toJson());
		}
		return b;
	}

	public int getReadingsCount() {
		return this.store.size();
	}
}
