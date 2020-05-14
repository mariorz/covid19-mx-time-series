# covid19-mx-time-series

This project generates time series data of the COVID-19 epidemic for every state in Mexico.

## Data Organization

### Basic
The basic set of files are located in the root [data directory](https://github.com/mariorz/covid19-mx-time-series/tree/master/data). These time series are aggregated by the date in which they were officially published, as opposed to symptom onset, or hospital admission date. They are also aggregated by the state in which the patient was treated, as opposed to their state of residence. These are probably the series that would make more sense to compare against other international time series, like the ones published by [JHU CSSE](https://github.com/CSSEGISandData/COVID-19).

filename | description
--------------|--------------
**covid19_deaths_mx.csv**|Confirmed deaths by date of official publication.
**covid19_confirmed_mx.csv**|Confirmed cases by date of official publication.
**covid19_negatives_mx.csv**|Cases that tested negative by date of official publication.
**covid19_suspects_mx.csv**|Suspect cases by date of official publication.

:warning: On dates of official publication
--------------|
It is worth noting that there is a considerable lag (up to 14 days) between when the patients are treated and when they are officially published as confirmed cases. These lags do seem to affect the shape of the resulting curves. Further analysis on this issue can be found [here](https://datos.nexos.com.mx/?p=1351).|

### Full
The full set of time series files are located in the [data/full directory](https://github.com/mariorz/covid19-mx-time-series/tree/master/data/full). 

These time series are aggregated either by the state in which the patient was treated in the [data/full/by_hospital_state directory](https://github.com/mariorz/covid19-mx-time-series/tree/master/data/full/by_hospital_state), or by their state of residence in the [data/full/by_residency_state directory](https://github.com/mariorz/covid19-mx-time-series/tree/master/data/full/by_residency_state). 

Each of these directories include the following time series CSV files according to case classification and the dates in which they are aggregated by. They are all updated using the latest published official db snapshot. This means that a case that started symptoms onset on date X, but had their case officially published as confirmed on date X+15, will be counted on date X for the files aggregated by date of symptoms onset. However, this update to date X will occur on day X+15.

filename | description
--------------|--------------
**confirmed_by_symptoms_date_mx.csv**|Confirmed cases aggregated by date of symptoms onset.
**suspects_by_symptoms_date_mx.csv**|Suspect cases aggregated by date of symptoms onset.
**negatives_by_symptoms_date_mx.csv**|Cases that tested negative aggregated by date of symptoms onset.
**hospitalized_confirmed_by_symptoms_date_mx.csv**|Hospitalized cases confirmed aggregated by date of symptoms onset.
**hospitalized_confirmed_by_admission_date_mx.csv**|Hospitalized cases confirmed aggregated by date of hospital admission.
**hospitalized_suspects_by_symptoms_date_mx.csv**|Hospitalized suspect cases aggregated by date of symptoms onset.
**hospitalized_suspects_by_admission_date_mx.csv**|Hospitalized suspect cases aggregated by date of hospital admission.
**deaths_confirmed_by_symptoms_date_mx.csv**|Confirmed deaths aggregated by date of symptoms onset.
**deaths_confirmed_by_admission_date_mx.csv**|Confirmed deaths aggregated by date of hospital admission.
**deaths_confirmed_by_death_date_mx.csv**|Confirmed deaths aggregated by date of death.
**deaths_suspects_by_symptoms_date_mx.csv**|Suspect deaths aggregated by date of symptoms onset.
**deaths_suspects_by_admission_date_mx.csv**|Suspect deaths aggregated by date of hospital admission.
**deaths_suspects_by_death_date_mx.csv**|Suspect deaths aggregated by date of death.
**deaths_negatives_by_symptoms_date_mx.csv**|Deaths that tested negative aggregated by date of symptoms onset.
**deaths_negatives_by_admission_date_mx.csv**|Deaths that tested negative aggregated by date of hospital admission.
**deaths_negatives_by_death_date_mx.csv**|Deaths that tested negative aggregated by date of death.



### Dates
Date format used is dd/mm/yyyy

## How we gather the data

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



#### Missing Data (Affecting only the series generated by date of official publication)
Data for days prior to when this project started running (02-04-2020) were reconstructed from [carranco-sga/Mexico-COVID-19](https://github.com/carranco-sga/Mexico-COVID-19), another project that transcribes oficial pdf data into CSV files, with the following caveats:
1) Missing suspect cases from 29-02-2020 to 13-03-2020, as per https://github.com/carranco-sga/Mexico-COVID-19/issues/1
2) There is no tracking of negative cases

#### Incorrect Data (Affecting only the series generated by date of official publication)
On April 12th, The Secretaría de Salud published data that contradicts the **negative cases** data published on the day before by showing a reduction in numbers for a few states. This appears to be some sort of error on their part as even their presented numbers for total studied cases [fail to add up correctly](https://pbs.twimg.com/media/EVfp5M7XsAAyCy1?format=jpg&name=medium).


