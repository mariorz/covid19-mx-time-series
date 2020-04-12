# covid19-mx-time-series

This project generates time series data for the covid19 epideic for every state in Mexico.

Mexico's Secretaría de Salud publishes daily tables (as PDFs) of confirmed and suspected cases. 
These files are already transcribed into more parsable formats by at least a couple of great projects:
* https://github.com/guzmart/covid19_mex
* https://serendipia.digital/2020/03/datos-abiertos-sobre-casos-de-coronavirus-covid-19-en-mexico/

Unfortunately these tables do not include deaths by state. 

Secretaría de Salud does publish an interactive map which include all the data for each state which they update daily at: https://ncov.sinave.gob.mx/mapa.aspx

We extract the JSON source file used by this map daily, and use that to generate CSV files that include all relevant counts for each state in Mexico as a time series.

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

#### To Do
* Get negative cases for previous days using other projects?
* Get mising suspects 29-02-2020 to 13-03-2020
* Add coordinates for each state
