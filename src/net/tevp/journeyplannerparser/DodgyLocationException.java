package net.tevp.journeyplannerparser;

public class DodgyLocationException extends ParseException
{
	public JourneyLocation original;
	DodgyLocationException() {super("Dodgy location specified!");}
	DodgyLocationException(String s) {super(s);}
	DodgyLocationException(JourneyLocation loc) {super("Dodgy location specified!");original=loc;}
}
