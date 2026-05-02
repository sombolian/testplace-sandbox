export interface CountryMeta {
  iso3: string
  iso2: string
  name: string
  officialName: string
  capital: string | null
  region: string
  subregion: string | null
  population: number
  area: number | null
  latlng: [number, number] | null
  languages: string[]
  currencies: { code: string; name: string; symbol?: string }[]
  flagEmoji: string
  demonym: string | null
  timezones: string[]
  independent: boolean
  unMember: boolean
  landlocked: boolean
  fifa: string | null
}

export type CountryMetaMap = Record<string, CountryMeta>

export type LayerMode = 'political' | 'physical' | 'night'
