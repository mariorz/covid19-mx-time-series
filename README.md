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


### To Do
* Get time series for previous days using other projects
* Is there any way to get the death counts by state for the previous days?
* Add coordinates for each state
