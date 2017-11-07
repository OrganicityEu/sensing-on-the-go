package eu.smartsantander.androidExperimentation.jsonEntities;


import java.util.ArrayList;
import java.util.List;

import gr.cti.android.experimentation.model.PluginDTO;

public class PluginList
{
	private List<PluginDTO> plugList;
	
	public PluginList()
	{
		plugList = new ArrayList<>();
	}
	
	public void setPluginList(List<PluginDTO> plugList)
	{
		this.plugList = plugList;
	}
	
	public List<PluginDTO> getPluginList()
	{
		return this.plugList;
	}
}
