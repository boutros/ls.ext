import React, { PropTypes } from 'react'

import SearchFilter from './SearchFilter'
import Labels from '../constants/Labels'

export default React.createClass({
  propTypes: {
    filtersByQuery: PropTypes.array.isRequired,
    locationQuery: PropTypes.object.isRequired,
    dispatch: PropTypes.func.isRequired
  },
  getDefaultProps () {
    return {
      filtersByQuery: []
    }
  },
  render () {
    let groupedFilters = {}
    if (this.props.filtersByQuery) {
      this.props.filtersByQuery.forEach(filter => {
        groupedFilters[ filter.aggregation ] = groupedFilters[ filter.aggregation ] || []
        groupedFilters[ filter.aggregation ].push(filter)
      })

      return (
        <aside className='col filters'>
          <h3>Avgrens søket ditt</h3>
          {Object.keys(groupedFilters).map(aggregation => {
            let filtersByAggregation = groupedFilters[ aggregation ]
            return (
              <SearchFilter
                key={aggregation}
                title={Labels[aggregation]}
                filters={filtersByAggregation}
                locationQuery={this.props.locationQuery}
                dispatch={this.props.dispatch}/>
            )
          })}
        </aside>
      )
    } else {
      return <div></div>
    }
  }
})