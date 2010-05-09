package net.tevp.JourneyPlannerParser;

import java.util.regex.*;
import java.io.*;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.text.DateFormatSymbols;
import java.util.*;
import java.net.URLEncoder;

public class JourneyPlannerParser
{
	public static void main(String [] args)
	{
		try
		{
			JourneyPlannerParser jpp = new JourneyPlannerParser(true);
			Vector<Journey> js;
			JourneyParameters jp = new JourneyParameters();
			jp.when = new Date(2010-1900, 5, 10, 0, 23);
			jp.speed = Speed.fast;
			//js = jpp.doJourney(LocationType.Postcode.create("E3 4AE"),LocationType.Postcode.create("SW7 2AZ"), jp);
			js = jpp.doJourney(LocationType.Stop.create("Kings Cross Rail Station"),LocationType.Postcode.create("E8 1JH"), jp);
			//js = jpp.doJourney(LocationType.Stop.create("Kings Cross"),LocationType.Postcode.create("E8 1JH"), jp);
			for (int i=0;i<js.size();i++)
			{
				System.out.println(i);
				System.out.println(js.get(i));
				System.out.println("");
			}
			assert js.size()!=0;
		}
		catch (AmbiguousLocationException e)
		{
			System.out.println(e.original);
			System.out.println(e.options);
			e.printStackTrace();
		}
		catch (ParseException e)
		{
			e.printStackTrace();
		}
	}

	Pattern route, tds, alt, departing, strip_link;
	Pattern walk_to, tube_to, tube_direct, bus_to;
	Pattern transit_time, payonboard;
	Pattern fieldset, legend, option;

	boolean debug;

	public JourneyPlannerParser(boolean _debug)
	{
		debug = _debug;

		route = Pattern.compile("<table class=\"routedetails\">(.+?)</table>", Pattern.DOTALL);
		tds = Pattern.compile("<td[^>]*?>(.*?)</td>", Pattern.DOTALL);
		alt = Pattern.compile("alt=\"([^\"]+)\"");
		departing = Pattern.compile("<strong>Departing:[^\n]+\n\\s+</strong>(\\S+)[^\n]*\n[^\n]*\n[^\\d]+(\\d+)[^\n]*\n\\s+(\\S+)\\s\n[^\n]*\n[^\\d]+(\\d+) at: (\\d+):(\\d+)</li>");
		strip_link = Pattern.compile("<a href=\"[^\"]+\">([^<]+)</a>");
		walk_to = Pattern.compile("Walk to (.+?)<br");
		tube_to = Pattern.compile("(?:T|t)ake(?: the )?(.+?<br /><br />)<span class=\"zoneinfo\">(?:Z|z)one\\(s\\): ([\\d, ]+)</span>", Pattern.DOTALL);
		tube_direct = Pattern.compile("<span class=\"[^\"]+\">([^<]+)</span> towards (.+?)<br");
		bus_to = Pattern.compile("Route (?:Express )?Bus ([A-Z\\d]+) from Stop:  ([\\S\\d]+)<br[^>]*> towards (.+?)<br");
		transit_time = Pattern.compile("time:\\s(\\d+).+?mins", Pattern.DOTALL);
		payonboard = Pattern.compile("<table cellspacing=\"0\".*?</table>");

		fieldset = Pattern.compile("<fieldset(.*?)</fieldset>", Pattern.DOTALL);
		legend = Pattern.compile("<legend>(.*?)</legend>");
		option = Pattern.compile("<option[^>]+>(.*?)</option>");
	}

	public Vector<Journey> doJourney(JourneyLocation start, JourneyLocation end, JourneyParameters params) throws ParseException
	{
		HashMap<String,String> m = new HashMap<String,String>();
		m.put("language","en");
		m.put("sessionID","0");
		m.put("requestID","0");
		m.put("ptOptionsActive","1");
		m.put("itOptionsActive","1");
		m.put("imparedOptionsActive","1");
		m.put("ptAdvancedOptions","1");
		m.put("advOptActive_2","1");
		m.put("advOpt_2","1");
		m.put("execInst","normal");
		m.put("command","");
		m.put("itdLPxx_request","");
		m.put("itdLPxx_view","");
		m.put("itdLPxx_tubeMap","");
		m.put("calculateDistance","1");
		m.put("imageFormat","png/pdf");
		m.put("imageOnly","1");
		m.put("imageWidth","705");
		m.put("imageHeight","500");
		m.put("calculateCO2","1");
		m.put("name_origin",start.data);
		m.put("nameState_origin","notidentified");
		m.put("nameDefaultText_origin","start");
		m.put("place_origin","London");
		m.put("type_origin",start.getTFLName());
		m.put("name_destination",end.data);
		m.put("nameState_destination","notidentified");
		m.put("nameDefaultText_destination","end");
		m.put("type_destination",end.getTFLName());
		m.put("place_destination","London");
		m.put("itdTripDateTimeDepArr",params.timeType.getDetails());

		Calendar time = new GregorianCalendar();
		time.setTime(params.when);
		m.put("itdDateDay",String.format("%02d", time.get(Calendar.DAY_OF_MONTH)));
		m.put("itdDateYearMonth", String.format("%4d%02d", time.get(Calendar.YEAR),time.get(Calendar.MONTH)));
		m.put("itdTimeHour",String.format("%02d", time.get(Calendar.HOUR_OF_DAY)));
		m.put("itdTimeMinute",String.format("%02d", time.get(Calendar.MINUTE)));

		m.put("Submit","Search");
		m.put("routeType",params.routeType.getDetails());
		m.put("nameDefaultText_via","Enter location+%28optional%29");
		m.put("nameState_via","notidentified");
		if (params.via == null)
		{
			m.put("name_via","Enter location+%28optional%29");
			m.put("type_via","stop");
		}
		else
		{
			m.put("name_via",params.via.data);
			m.put("type_via",params.via.getTFLName());
		}
		m.put("place_via","London");
		m.put("placeDefaultText_via","London");
		m.put("includedMeans","checkbox");
		m.put("inclMOT_11","1");
		m.put("inclMOT_0","on");
		m.put("inclMOT_1","on");
		m.put("inclMOT_2","on");
		m.put("inclMOT_4","on");
		m.put("inclMOT_5","on");
		m.put("inclMOT_7","on");
		m.put("inclMOT_9","on");
		m.put("trITMOTvalue101","60");
		m.put("trITMOTvalue","20");
		m.put("trITMOT","100");
		m.put("changeSpeed",params.speed.toString());
		m.put("tripSelection","on");
		m.put("itdLPxx_view","detail");
		m.put("ptOptionsActive","1");
		m.put("calculateDistance","1");
		m.put("tripSelector1","on");
		m.put("tripSelector2","on");
		m.put("tripSelector3","on");
		m.put("tripSelector4","on");
		m.put("tripSelector5","on");
		m.put("tripSelector6","on");
		m.put("tripSelector7","on");
		m.put("Submit","View selected");

		StringBuffer sb = new StringBuffer();
		for (String key: m.keySet())
		{
			if (sb.length()>0)
				sb.append("&");
			try {
				sb.append(String.format("%s=%s", key, URLEncoder.encode(m.get(key),"UTF-8")));
			}
			catch (UnsupportedEncodingException e)
			{
				throw new ParseException("Error trying to encode '"+m.get(key)+"'");
			}
		}
		if (debug)
			System.out.println(sb.toString());

		BufferedURLConnection buc = null;
		try 
		{
			buc = new BufferedURLConnection("http://journeyplanner.tfl.gov.uk/user/XSLT_TRIP_REQUEST2", sb.toString());
		}
		catch (IOException e)
		{
			throw new ParseException("IOException trying to get data from TfL: "+e.getMessage());
		}
		if (debug)
			System.out.println(buc.headers);

		if (debug)
		{
			try
			{
				FileOutputStream fs = new FileOutputStream("dump.html");
				fs.write(buc.outputData.getBytes(),0,buc.outputData.length());
				fs.close();
			}
			catch (FileNotFoundException e)
			{
				throw new ParseException("Failure to write dump.html for debug: "+e.getMessage());
			}
			catch (IOException e)
			{
				throw new ParseException("IOException while writing dump.html for debug: "+e.getMessage());
			}
		}

		return parseString(start,end,buc.outputData);
	}

	public Vector<Journey> parseString(String data) throws ParseException
	{
		return parseString(null, null, data);
	}

	public Vector<Journey> parseString(JourneyLocation start, JourneyLocation end, String data) throws ParseException
	{
		Vector<Journey> res = new Vector<Journey>();
		
		Matcher d = departing.matcher(data);
		if (!d.find())
		{
			Matcher field = fieldset.matcher(data);
			if (field.find())
			{
				AmbiguousLocationException ale = new AmbiguousLocationException();
				Matcher leg = legend.matcher(field.group(1));
				if (leg.find())
				{
					if (leg.group(1).compareTo("From")==0)
						ale.original = start;
					else if (leg.group(1).compareTo("Travelling to...")==0)
						ale.original = end;
					else
						throw new ParseException(leg.group(1));
				}
				else
					throw new ParseException(field.group(1));

				Matcher opts = option.matcher(field.group(1));
				ale.options = new Vector<String>();
				while (opts.find())
				{
					ale.options.add(opts.group(1));
				}
				if (ale.options.size()==0)
					throw new ParseException(field.group(1));
					
				throw ale;
			}
		}
		//System.out.println(d.group(0));

		Calendar base = new GregorianCalendar();
		base.set(Calendar.DAY_OF_MONTH, Integer.parseInt(d.group(2)));
		DateFormatSymbols dfs = new DateFormatSymbols();
		
		base.set(Calendar.MONTH, Arrays.asList(dfs.getMonths()).indexOf(d.group(3)));
		base.set(Calendar.YEAR, Integer.parseInt(d.group(4)));
		base.set(Calendar.HOUR, Integer.parseInt(d.group(5)));
		base.set(Calendar.MINUTE, Integer.parseInt(d.group(6)));
		base.set(Calendar.SECOND, 0);
		//System.out.println(base.getTime());
		
		Matcher pb = payonboard.matcher(data);

		Matcher r = route.matcher(pb.replaceAll(""));
		while (r.find())
		{
			//System.out.println(r.group(1));
			if (debug)
				System.out.println("New Match\n=========");
			Matcher tdlist = tds.matcher(r.group(1));
			int type = 0;
			Journey j = new Journey();
			JourneySegment js = null;
			boolean end_of_journey = false;
			while (tdlist.find())
			{	
				if (!end_of_journey)
				{
					if (debug)
					{
						System.out.print("Type: ");
						System.out.print(type);
						System.out.print(" - ");
						System.out.println(tdlist.group(1));
						System.out.println("");
					}
					switch(type)
					{
						case 0: // time and type
						{
							//System.out.println(tdlist.group(1));
							Matcher a = alt.matcher(tdlist.group(1));
							if (a.find())
							{
								js = new JourneySegment();
								if (a.group(1).equals("Walk"))
									js.type = TransportType.Walk;
								else if (a.group(1).equals("Tube"))
									js.type = TransportType.Tube;
								else if (a.group(1).equals("Bus"))
									js.type = TransportType.Bus;
								else
									throw new ParseException("Unknown transit type: "+a.group(1));

								if (tdlist.group(1).indexOf(":")!=-1)
								{
									if (debug)
										System.out.println("time parse: "+tdlist.group(1));
									Calendar ts = Calendar.getInstance();
									ts.setTime(base.getTime());
									int hour = Integer.parseInt(tdlist.group(1).substring(0,2));
									if (hour<base.get(Calendar.HOUR_OF_DAY))
										hour += 24;
									ts.set(Calendar.HOUR_OF_DAY, hour);
									ts.set(Calendar.MINUTE, Integer.parseInt(tdlist.group(1).substring(3,5)));
									js.time_start = ts.getTime();
									
									ts = Calendar.getInstance();
									ts.setTime(base.getTime());
									try
									{
										int len = tdlist.group(1).length();
										hour = Integer.parseInt(tdlist.group(1).substring(len-5,len-3));
										if (hour<base.get(Calendar.HOUR_OF_DAY))
											hour += 24;
										ts.set(Calendar.HOUR_OF_DAY, hour);
										ts.set(Calendar.MINUTE, Integer.parseInt(tdlist.group(1).substring(len-2,len)));
										js.time_end = ts.getTime();

										if (j.size()>0 && j.last().time_end == null)
											j.last().time_end = (Date)js.time_start.clone();
									}
									catch (NumberFormatException e)
									{
										assert tdlist.group(1).substring(5).indexOf(":")==-1;
									}
								}
								else
								{
									if (j.last().time_end != null)
										js.time_start = (Date)j.last().time_end.clone();
								}
							}
							else
							{
								if (tdlist.group(1).indexOf(":")!=-1)
								{
									Calendar ts = Calendar.getInstance();
									ts.setTime(base.getTime());
									int hour = Integer.parseInt(tdlist.group(1).substring(0,2));
									if (hour<base.get(Calendar.HOUR_OF_DAY))
										hour += 24;
									ts.set(Calendar.HOUR_OF_DAY, hour);
									ts.set(Calendar.MINUTE, Integer.parseInt(tdlist.group(1).substring(3,5)));
									j.last().time_end = ts.getTime();
								}
								end_of_journey = true;
								j.corrections();
								res.add(j);
							}
							break;
						}
						case 1:
						{
							if (debug)
							{
								System.out.println("");
								System.out.println(tdlist.group(1));
							}
							String segment = tdlist.group(1);
							segment = segment.substring(0,segment.indexOf("<br"));
							if (segment.indexOf("<a")!=-1)
							{
								Matcher rep = strip_link.matcher(segment);
								rep.find();
								segment = rep.group(1);
							}
							js.loc_start = segment;
							switch (js.type)
							{
								case Walk:
								{
									Matcher w = walk_to.matcher(tdlist.group(1));
									w.find();
									js.loc_end = w.group(1);
									break;
								}
								case Tube:
								{
									Matcher t = tube_to.matcher(tdlist.group(1));
									t.find();
									Matcher t2 = tube_direct.matcher(t.group(1));
									if (debug)
										System.out.println("Searching for tube from: "+t.group(1));
									while (t2.find())
									{
										Route ro = new Route();
										ro.thing = t2.group(1);
										ro.towards = t2.group(2);
										ro.stop = null;
										js.routes.add(ro);
									}
									break;
								}
								case Bus:
								{
									Matcher b = bus_to.matcher(tdlist.group(1));
									while (b.find())
									{
										Route ro = new Route();
										ro.thing = b.group(1);
										ro.stop = b.group(2);
										ro.towards = b.group(3);
										js.routes.add(ro);
									}
									break;
								}
							}
							//System.out.println(js);
							break;
						}
						case 2:
						{
							Matcher time = transit_time.matcher(tdlist.group(1));
							time.find();
							js.minutes = Integer.parseInt(time.group(1));
							Matcher alts = alt.matcher(tdlist.group(1));
							while (alts.find())
							{
								if (alts.group(1).equals("stairs up"))
									js.impediments.add(Impediments.StairsUp);
								else if (alts.group(1).equals("stairs down"))
									js.impediments.add(Impediments.StairsDown);
								else if (alts.group(1).equals("lift up"))
									js.impediments.add(Impediments.LiftUp);
								else if (alts.group(1).equals("escalator up"))
									js.impediments.add(Impediments.EscalatorUp);
								else
									throw new ParseException("Unknown impediment type: "+alts.group(1));
							}
							break;
						}

						case 3:
							//System.out.println(js);
							j.add(js);
							break;
					}
				}
				type = (type +1) % 4;
				if (end_of_journey && type == 0)
					end_of_journey = false;
			}
			assert js!=null;
		}
		assert res.size()!=0;
		return res;
	}
}


