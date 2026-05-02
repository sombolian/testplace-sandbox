/**
 * Curated geopolitics overlay sets.  Each overlay maps to an ISO Alpha-3 set.
 * Membership reflects 2024–2025 status.  Educational, not exhaustive.
 */

export interface Overlay {
  id: string
  label: string
  description: string
  color: string // tint applied when active
  members: string[]
}

export const OVERLAYS: Overlay[] = [
  {
    id: 'nato',
    label: 'NATO',
    description: 'North Atlantic Treaty Organization — collective defence alliance (32 members).',
    color: '#3b82f6',
    members: [
      'ALB', 'BEL', 'BGR', 'CAN', 'HRV', 'CZE', 'DNK', 'EST', 'FIN', 'FRA', 'DEU', 'GRC',
      'HUN', 'ISL', 'ITA', 'LVA', 'LTU', 'LUX', 'MNE', 'NLD', 'MKD', 'NOR', 'POL', 'PRT',
      'ROU', 'SVK', 'SVN', 'ESP', 'SWE', 'TUR', 'GBR', 'USA',
    ],
  },
  {
    id: 'eu',
    label: 'European Union',
    description: 'Political and economic union of 27 European member states.',
    color: '#fbbf24',
    members: [
      'AUT', 'BEL', 'BGR', 'HRV', 'CYP', 'CZE', 'DNK', 'EST', 'FIN', 'FRA', 'DEU', 'GRC',
      'HUN', 'IRL', 'ITA', 'LVA', 'LTU', 'LUX', 'MLT', 'NLD', 'POL', 'PRT', 'ROU', 'SVK',
      'SVN', 'ESP', 'SWE',
    ],
  },
  {
    id: 'brics',
    label: 'BRICS+',
    description: 'Intergovernmental organization (Brazil, Russia, India, China, South Africa + expansion).',
    color: '#ef4444',
    members: ['BRA', 'RUS', 'IND', 'CHN', 'ZAF', 'EGY', 'ETH', 'IRN', 'ARE'],
  },
  {
    id: 'commonwealth',
    label: 'Commonwealth',
    description: 'Commonwealth of Nations — 56 member states, mostly former British Empire territories.',
    color: '#8b5cf6',
    members: [
      'ATG', 'AUS', 'BHS', 'BGD', 'BRB', 'BLZ', 'BWA', 'BRN', 'CMR', 'CAN', 'CYP', 'DMA',
      'SWZ', 'FJI', 'GAB', 'GMB', 'GHA', 'GRD', 'GUY', 'IND', 'JAM', 'KEN', 'KIR', 'LSO',
      'MWI', 'MYS', 'MDV', 'MLT', 'MUS', 'MOZ', 'NAM', 'NRU', 'NZL', 'NGA', 'PAK', 'PNG',
      'RWA', 'KNA', 'LCA', 'VCT', 'WSM', 'SYC', 'SLE', 'SGP', 'SLB', 'ZAF', 'LKA', 'TZA',
      'TGO', 'TON', 'TTO', 'TUV', 'UGA', 'GBR', 'VUT', 'ZMB',
    ],
  },
  {
    id: 'opec',
    label: 'OPEC+',
    description: 'Organization of the Petroleum Exporting Countries and aligned producers.',
    color: '#10b981',
    members: [
      'DZA', 'AGO', 'COG', 'GNQ', 'GAB', 'IRN', 'IRQ', 'KWT', 'LBY', 'NGA', 'SAU', 'ARE',
      'VEN', 'AZE', 'BHR', 'BRN', 'KAZ', 'MYS', 'MEX', 'OMN', 'RUS', 'SDN', 'SSD',
    ],
  },
  {
    id: 'g7',
    label: 'G7',
    description: 'Group of Seven — Canada, France, Germany, Italy, Japan, UK, USA.',
    color: '#f97316',
    members: ['CAN', 'FRA', 'DEU', 'ITA', 'JPN', 'GBR', 'USA'],
  },
  {
    id: 'g20',
    label: 'G20',
    description: 'Group of Twenty — major advanced & emerging economies (incl. EU & AU).',
    color: '#a855f7',
    members: [
      'ARG', 'AUS', 'BRA', 'CAN', 'CHN', 'FRA', 'DEU', 'IND', 'IDN', 'ITA', 'JPN', 'MEX',
      'KOR', 'RUS', 'SAU', 'ZAF', 'TUR', 'GBR', 'USA',
    ],
  },
  {
    id: 'asean',
    label: 'ASEAN',
    description: 'Association of Southeast Asian Nations.',
    color: '#06b6d4',
    members: ['BRN', 'KHM', 'IDN', 'LAO', 'MYS', 'MMR', 'PHL', 'SGP', 'THA', 'VNM'],
  },
  {
    id: 'au',
    label: 'African Union',
    description: 'Continental union of 55 African states.',
    color: '#facc15',
    members: [
      'DZA', 'AGO', 'BEN', 'BWA', 'BFA', 'BDI', 'CMR', 'CPV', 'CAF', 'TCD', 'COM', 'COG',
      'COD', 'CIV', 'DJI', 'EGY', 'GNQ', 'ERI', 'SWZ', 'ETH', 'GAB', 'GMB', 'GHA', 'GIN',
      'GNB', 'KEN', 'LSO', 'LBR', 'LBY', 'MDG', 'MWI', 'MLI', 'MRT', 'MUS', 'MAR', 'MOZ',
      'NAM', 'NER', 'NGA', 'RWA', 'STP', 'SEN', 'SYC', 'SLE', 'SOM', 'ZAF', 'SSD', 'SDN',
      'TZA', 'TGO', 'TUN', 'UGA', 'ZMB', 'ZWE', 'ESH',
    ],
  },
  {
    id: 'mercosur',
    label: 'Mercosur',
    description: 'South American trade bloc.',
    color: '#22c55e',
    members: ['ARG', 'BRA', 'PRY', 'URY', 'BOL'],
  },
  {
    id: 'arabLeague',
    label: 'Arab League',
    description: 'League of Arab States — 22 member states.',
    color: '#0ea5e9',
    members: [
      'DZA', 'BHR', 'COM', 'DJI', 'EGY', 'IRQ', 'JOR', 'KWT', 'LBN', 'LBY', 'MRT', 'MAR',
      'OMN', 'PSE', 'QAT', 'SAU', 'SOM', 'SDN', 'SYR', 'TUN', 'ARE', 'YEM',
    ],
  },
]

export const OVERLAYS_BY_ID: Record<string, Overlay> = Object.fromEntries(
  OVERLAYS.map((o) => [o.id, o]),
)
