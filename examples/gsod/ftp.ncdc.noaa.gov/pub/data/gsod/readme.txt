                      FEDERAL CLIMATE COMPLEX
                GLOBAL SURFACE SUMMARY OF DAY DATA                       
                           VERSION 7
                  (OVER 9000 WORLDWIDE STATIONS)  
                           09/08/2010

********************************************************************

SPECIAL NOTES 

The data summaries provided here are based on data exchanged under
the World Meteorological Organization (WMO) World Weather Watch Program
according to WMO Resolution 40 (Cg-XII).  This allows WMO member 
countries to place restrictions on the use or re-export of their data 
for commercial purposes outside of the receiving country.  Data for 
selected countries may, at times, not be available through this system.   

Those countries' data summaries and products which are available
here are intended for free and unrestricted use in research,
education, and other non-commercial activities.  However, for
non-U.S. locations' data, the data or any derived product shall
not be provided to other users or be used for the re-export of
commercial services.  To determine off-line availability of any
country's data, please contact NCDC--ncdc.orders@noaa.gov,
828-271-4800.  Please email ncdc.info@noaa.gov if you have
any other questions.

As described below, the data are available via: 
WWW -- http://www.ncdc.noaa.gov/cgi-bin/res40.pl?page=gsod.html
and FTP -- ftp://ftp.ncdc.noaa.gov/pub/data/gsod
and...the WWW system includes graphing and selection of
data by station and element.

********************************************************************

OVERVIEW

The following is a description of the global surface summary
of day product produced by the National Climatic Data Center (NCDC)
in Asheville, NC.  The input data used in building these daily
summaries are the Integrated Surface Data (ISD), which includes
global data obtained from the USAF Climatology Center, located
in the Federal Climate Complex with NCDC.  The latest daily summary
data are normally available 1-2 days after the date-time of the 
observations used in the daily summaries.  The online data files begin
with 1929, and are now at the Version 7 software level.  Over 9000
stations' data are typically available.  

The daily elements included in the dataset (as available from each
station) are:

Mean temperature (.1 Fahrenheit)
Mean dew point (.1 Fahrenheit)
Mean sea level pressure (.1 mb)
Mean station pressure (.1 mb)
Mean visibility (.1 miles)
Mean wind speed (.1 knots)
Maximum sustained wind speed (.1 knots)
Maximum wind gust (.1 knots)
Maximum temperature (.1 Fahrenheit)
Minimum temperature (.1 Fahrenheit)
Precipitation amount (.01 inches)
Snow depth (.1 inches)
Indicator for occurrence of:  Fog
                              Rain or Drizzle
                              Snow or Ice Pellets
                              Hail
                              Thunder
                              Tornado/Funnel Cloud

For details on the contents of the dataset, see the format
documentation shown below.  

The data are available via:
1) WWW -- http://www.ncdc.noaa.gov/cgi-bin/res40.pl?page=gsod.html
2) FTP -- ftp://ftp.ncdc.noaa.gov/pub/data/gsod  via browser
3) Command line ftp:

a) Enter:  open ftp.ncdc.noaa.gov    

b) Login is:  ftp

c) Password is:  your email address

d) To move to the correct subdirectory, enter:  
   cd /pub/data/gsod  

   The files included in this subdirectory are:

   Data Files--

   Annual files:
   eg, gsod_2006.tar - All 2006 files (compressed) by station, in one tar file.
   etc, etc - For each annual volume.
   Note:  Each year's data are contained in subdirectories/folders by year.   

   Station files:
   eg, 010010-99999-2006.op.gz - Files by station year, identified by WMO number, 
   WBAN number (if appropriate), and year.  For a cross reference of the 
   filenames with location, see:
   ish-history.txt 
  
   Informational/Utility Files--
   
   country-list.txt - A list showing the station number range for
                      each country.

   ish-history.txt -- A station list to be used with the data files,
                      showing the names and locations for each station.
                      Note:  Global summary of day contains a subset of the 
                      stations listed in this station history.

   readme.txt - A description of the data and its format.
 
e) To get a copy of the data description, enter: 
   get readme.txt destination  (destination is your           
   output location and name)...e.g.--
   get readme.txt c:readme.txt - copies to hard drive c:

f) Then, to get a copy of any of the other files, use
   the same procedure, such as--
   get gsod_2006.tar c:data.txt

g) To logoff the system when finished, enter:
   bye

********************************************************************

DETAILS/FORMAT

Global summary of day data for 18 surface meteorological elements
are derived from the synoptic/hourly observations contained in
USAF DATSAV3 Surface data and Federal Climate Complex Integrated 
Surface Data (ISD).  Historical data are generally available for 1929 to
the present, with data from 1973 to the present being the most complete.  
For some periods, one or more countries' data may not be available due to
data restrictions or communications problems.  In deriving the summary of 
day data, a minimum of 4 observations for the day must be present (allows 
for stations which report 4 synoptic observations/day).  Since the data are
converted to constant units (e.g, knots), slight rounding error from the
originally reported values may occur (e.g, 9.9 instead of 10.0).

The mean daily values described below are based on the hours of
operation for the station.  For some stations/countries, the
visibility will sometimes 'cluster' around a value (such as 10
miles) due to the practice of not reporting visibilities greater
than certain distances.  The daily extremes and totals--maximum
wind gust, precipitation amount, and snow depth--will only appear
if the station reports the data sufficiently to provide a valid value.
Therefore, these three elements will appear less frequently than 
other values.  Also, these elements are derived from the stations'
reports during the day, and may comprise a 24-hour period which
includes a portion of the previous day.  The data are reported and
summarized based on Greenwich Mean Time (GMT, 0000Z - 2359Z) since
the original synoptic/hourly data are reported and based on GMT.

As for quality control (QC), the input data undergo extensive
automated QC to correctly 'decode' as much of the synoptic data as
possible, and to eliminate many of the random errors found in the
original data.  Then, these data are QC'ed further as the summary of
day data are derived.  However, we expect that a very small % of the
errors will remain in the summary of day data.

The data are strictly ASCII, with a mixture of character data, real
values, and integer values. 

Following is the data format:

First record--header record.
All ensuing records--data records as described below.
All 9's in a field (e.g., 99.99 for PRCP) indicates no report or
insufficient data.

FIELD   POSITION  TYPE   DESCRIPTION

STN---  1-6       Int.   Station number (WMO/DATSAV3 number)
                         for the location.

WBAN    8-12      Int.   WBAN number where applicable--this is the
                         historical "Weather Bureau Air Force Navy"
                         number - with WBAN being the acronym.

YEAR    15-18     Int.   The year.

MODA    19-22     Int.   The month and day.

TEMP    25-30     Real   Mean temperature for the day in degrees
                         Fahrenheit to tenths.  Missing = 9999.9
Count   32-33     Int.   Number of observations used in 
                         calculating mean temperature.

DEWP    36-41     Real   Mean dew point for the day in degrees
                         Fahrenheit to tenths.  Missing = 9999.9
Count   43-44     Int.   Number of observations used in 
                         calculating mean dew point.  

SLP     47-52     Real   Mean sea level pressure for the day
                         in millibars to tenths.  Missing =       
                         9999.9
Count   54-55     Int.   Number of observations used in 
                         calculating mean sea level pressure.

STP     58-63     Real   Mean station pressure for the day
                         in millibars to tenths.  Missing =       
                         9999.9
Count   65-66     Int.   Number of observations used in 
                         calculating mean station pressure.  

VISIB   69-73     Real   Mean visibility for the day in miles
                         to tenths.  Missing = 999.9
Count   75-76     Int.   Number of observations used in 
                         calculating mean visibility.      

WDSP    79-83     Real   Mean wind speed for the day in knots
                         to tenths.  Missing = 999.9 
Count   85-86     Int.   Number of observations used in 
                         calculating mean wind speed.

MXSPD   89-93     Real   Maximum sustained wind speed reported 
                         for the day in knots to tenths.
                         Missing = 999.9

GUST    96-100    Real   Maximum wind gust reported for the day
                         in knots to tenths.  Missing = 999.9

MAX     103-108   Real   Maximum temperature reported during the 
                         day in Fahrenheit to tenths--time of max 
                         temp report varies by country and        
                         region, so this will sometimes not be    
                         the max for the calendar day.  Missing = 
                         9999.9     
Flag    109-109   Char   Blank indicates max temp was taken from the
                         explicit max temp report and not from the              
                         'hourly' data.  * indicates max temp was 
                         derived from the hourly data (i.e., highest
                         hourly or synoptic-reported temperature).

MIN     111-116   Real   Minimum temperature reported during the 
                         day in Fahrenheit to tenths--time of min 
                         temp report varies by country and        
                         region, so this will sometimes not be  
                         the min for the calendar day.  Missing = 
                         9999.9
Flag    117-117   Char   Blank indicates min temp was taken from the
                         explicit min temp report and not from the              
                         'hourly' data.  * indicates min temp was 
                         derived from the hourly data (i.e., lowest
                         hourly or synoptic-reported temperature).

PRCP    119-123   Real   Total precipitation (rain and/or melted
                         snow) reported during the day in inches
                         and hundredths; will usually not end 
                         with the midnight observation--i.e., 
                         may include latter part of previous day.
                         .00 indicates no measurable              
                         precipitation (includes a trace).        
                         Missing = 99.99
                         Note:  Many stations do not report '0' on
                         days with no precipitation--therefore,  
                         '99.99' will often appear on these days.
                         Also, for example, a station may only
                         report a 6-hour amount for the period 
                         during which rain fell.
                         See Flag field for source of data.
Flag    124-124   Char   A = 1 report of 6-hour precipitation 
                             amount.
                         B = Summation of 2 reports of 6-hour 
                             precipitation amount.
                         C = Summation of 3 reports of 6-hour 
                             precipitation amount.
                         D = Summation of 4 reports of 6-hour 
                             precipitation amount.
                         E = 1 report of 12-hour precipitation
                             amount.
                         F = Summation of 2 reports of 12-hour
                             precipitation amount.
                         G = 1 report of 24-hour precipitation
                             amount.
                         H = Station reported '0' as the amount
                             for the day (eg, from 6-hour reports),
                             but also reported at least one
                             occurrence of precipitation in hourly
                             observations--this could indicate a
                             trace occurred, but should be considered
                             as incomplete data for the day.
                         I = Station did not report any precip data
                             for the day and did not report any
                             occurrences of precipitation in its hourly
                             observations--it's still possible that
                             precip occurred but was not reported.

SNDP    126-130   Real   Snow depth in inches to tenths--last     
                         report for the day if reported more than
                         once.  Missing = 999.9
                         Note:  Most stations do not report '0' on
                         days with no snow on the ground--therefore,
                         '999.9' will often appear on these days.

FRSHTT  133-138   Int.   Indicators (1 = yes, 0 = no/not          
                         reported) for the occurrence during the 
                         day of:
                         Fog ('F' - 1st digit).
                         Rain or Drizzle ('R' - 2nd digit).
                         Snow or Ice Pellets ('S' - 3rd digit).
                         Hail ('H' - 4th digit).
                         Thunder ('T' - 5th digit).
                         Tornado or Funnel Cloud ('T' - 6th       
                         digit).

********************************************************************

REFERENCE

The NCDC Climate Services Branch (CSB) is responsible for
distribution of NCDC products to users.  NCDC's CSB can be
contacted via the following phone number, internet address, or 
fax number.  

Telephone Number:   828-271-4800
Fax Number:         828-271-4876
Internet Address:   ncdc.orders@noaa.gov

********************************************************************

Neal Lott
NCDC/DAB

