import { combineReducers } from 'redux'
import { routeReducer } from 'react-router-redux'

import search from './search'
import resources from './resources'

const rootReducer = combineReducers(Object.assign({}, { search, resources }, {
  routing: routeReducer
}))

export default rootReducer
