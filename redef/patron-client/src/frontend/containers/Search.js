import React, { PropTypes } from 'react'
import { bindActionCreators } from 'redux'
import { connect } from 'react-redux'

import * as SearchActions from '../actions/SearchActions'
import SearchResults from '../components/SearchResults'
import SearchFilters from '../components/SearchFilters'

const Search = React.createClass({
  propTypes: {
    searchActions: PropTypes.object.isRequired,
    searchResults: PropTypes.array.isRequired,
    isSearching: PropTypes.bool.isRequired,
    dispatch: PropTypes.func.isRequired,
    searchError: PropTypes.any.isRequired,
    filters: PropTypes.array.isRequired,
    location: PropTypes.object.isRequired,
    searchFieldInput: PropTypes.string,
    totalHits: PropTypes.number.isRequired
  },
  render () {
    return (
      <div className='container'>
        <div className='row'>
          <SearchFilters filters={this.props.filters}
                         locationQuery={this.props.location.query} dispatch={this.props.dispatch}/>
          <SearchResults
            locationQuery={this.props.location.query}
            searchActions={this.props.searchActions}
            searchResults={this.props.searchResults}
            totalHits={this.props.totalHits}
            searchError={this.props.searchError}/>
        </div>
      </div>
    )
  }
})

function mapStateToProps (state) {
  return {
    searchResults: state.search.searchResults,
    totalHits: state.search.totalHits,
    isSearching: state.search.isSearching,
    searchError: state.search.searchError,
    filters: state.search.filters
  }
}

function mapDispatchToProps (dispatch) {
  return {
    searchActions: bindActionCreators(SearchActions, dispatch),
    dispatch: dispatch
  }
}

export default connect(
  mapStateToProps,
  mapDispatchToProps
)(Search)
