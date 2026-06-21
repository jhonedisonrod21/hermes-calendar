package co.com.hermes.calendar.catalog.search;

import co.com.hermes.calendar.catalog.offering.Modality;

/** Criterios de la búsqueda pública de servicios (todos opcionales; vacío = explorar todo). */
public record OfferingSearchCriteria(String q, String category, Modality modality) {
}
