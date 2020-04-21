# covid19-mx-time-series

This project generates time series data for the COVID-19 epidemic for every state in Mexico.

The techniques we use for generating the time series have evolved as the availability of oficial and semi-oficial data has changed.

At first Mexico's Secretaría de Salud published only daily reports (as PDFs) of confirmed and suspected cases. 

These files were readily transcribed into more parsable formats by at least a couple of great projects:
* https://github.com/guzmart/covid19_mex
* https://serendipia.digital/2020/03/datos-abiertos-sobre-casos-de-coronavirus-covid-19-en-mexico/

Unfortunately these reports did not include deaths for each state in Mexico. 

Secretaría de Salud also published an interactive map which included the complete accumulated tolls for each state, which they would update daily at: https://ncov.sinave.gob.mx/mapa.aspx

We extracted the JSON source file used by this map daily, and used that to generate CSV files that include all relevant counts for each state in Mexico as time series.

As of March 20th, this map, and its JSON sourced file, has been replaced by a new map which only displays information for the different incidence rates, but no total counts.

However, Secretaría de Salud has stared publishing a daily snapshot file with the disaggregated information for each patient case it studies at https://www.gob.mx/salud/documentos/datos-abiertos-152127

We now use these daily snapshots to generate our different time-series.

The files are located in the [data directory](https://github.com/mariorz/covid19-mx-time-series/tree/master/data)
* covid19_deaths_mx.csv
* covid19_confirmed_mx.csv
* covid19_negatives_mx.csv
* covid19_suspects_mx.csv

Date format used is dd/mm/yyyy

#### Missing Data 
Data for days prior to when this project started running (02-04-2020) were reconstructed from [carranco-sga/Mexico-COVID-19](https://github.com/carranco-sga/Mexico-COVID-19), another project that transcribes oficial pdf data into csvs, with the following caveats:
1) Missing suspect cases from 29-02-2020 to 13-03-2020, as per https://github.com/carranco-sga/Mexico-COVID-19/issues/1
2) Thare is no tracking of negative cases

#### Incorrect Data
On April 12th, The Secretaría de Salud published data that contradicts the **negative cases** data published on the day before by showing a reduction in numbers for a few states. This appears to be some sort of error on their part as even their presented numbers for total studied cases [fail to add up correctly](https://pbs.twimg.com/media/EVfp5M7XsAAyCy1?format=jpg&name=medium).

#### To Do
* Get negative cases for previous days using other projects?
* Get mising suspects 29-02-2020 to 13-03-2020
* Add coordinates for each state
