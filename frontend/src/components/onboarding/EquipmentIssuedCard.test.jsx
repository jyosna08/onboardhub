import { render, screen } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import EquipmentIssuedCard from './EquipmentIssuedCard.jsx'
import { ASSET_TYPE, TASK_CATEGORY, TASK_STATUS } from '@/utils/constants.js'

describe('EquipmentIssuedCard', () => {
  it('shows completed equipment tag, label and type icon', () => {
    const { container } = render(<EquipmentIssuedCard tasks={[{ id: 1, category: TASK_CATEGORY.EQUIPMENT, status: TASK_STATUS.DONE, assetType: ASSET_TYPE.LAPTOP, assetTag: 'LAP-0001', assetLabel: 'Dell Latitude' }]}/>);
    expect(screen.getByText('LAP-0001')).toBeInTheDocument()
    expect(screen.getByText('Dell Latitude')).toBeInTheDocument()
    expect(container.querySelector('svg')).toBeInTheDocument()
  })
  it('shows the empty equipment state', () => {
    render(<EquipmentIssuedCard tasks={[]}/>);
    expect(screen.getByText('No equipment issued yet')).toBeInTheDocument()
  })
})
